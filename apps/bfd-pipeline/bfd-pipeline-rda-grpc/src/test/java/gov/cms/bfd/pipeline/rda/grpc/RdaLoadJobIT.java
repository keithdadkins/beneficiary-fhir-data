package gov.cms.bfd.pipeline.rda.grpc;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.model.rda.RdaMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.server.ExceptionMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.JsonMessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.MessageSource;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaServer;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaSourceConfig;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RdaLoadJobIT {
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(60_000L), ZoneOffset.UTC);
  private static final CharSource fissClaimsSource =
      Resources.asCharSource(Resources.getResource("FISS.ndjson"), StandardCharsets.UTF_8);
  private static final CharSource mcsClaimsSource =
      Resources.asCharSource(Resources.getResource("MCS.ndjson"), StandardCharsets.UTF_8);
  private static final int BATCH_SIZE = 17;

  private ImmutableList<String> fissClaimJson;
  private ImmutableList<String> mcsClaimJson;

  @BeforeEach
  public void setUp() throws Exception {
    if (fissClaimJson == null) {
      fissClaimJson = fissClaimsSource.readLines();
    }
    if (mcsClaimJson == null) {
      mcsClaimJson = mcsClaimsSource.readLines();
    }
  }

  /**
   * All of our test claims should be valid for our IT tests to succeed. This test ensures this is
   * the case and catches any incompatibility issues when a new RDA API version contains breaking
   * changes.
   */
  @Test
  public void fissClaimsAreValid() throws Exception {
    final ImmutableList<FissClaimChange> expectedClaims =
        JsonMessageSource.parseAll(fissClaimJson, JsonMessageSource::parseFissClaimChange);
    final FissClaimTransformer transformer =
        new FissClaimTransformer(clock, MbiCache.computedCache(new IdHasher.Config(1, "testing")));
    for (FissClaimChange claim : expectedClaims) {
      try {
        transformer.transformClaim(claim);
      } catch (DataTransformer.TransformationException ex) {
        fail(String.format("bad sample claim: seq=%d error=%s", claim.getSeq(), ex.getErrors()));
      }
    }
  }

  @Test
  public void fissClaimsTest() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        RdaLoadJobIT.class,
        clock,
        (appState, entityManager) -> {
          assertTablesAreEmpty(entityManager);
          RdaServer.LocalConfig.builder()
              .fissSourceFactory(fissJsonSource(fissClaimJson))
              .build()
              .runWithPortParam(
                  port -> {
                    final RdaLoadOptions config = createRdaLoadOptions(port);
                    final PipelineJob<?> job = config.createFissClaimsLoadJob(appState);
                    job.call();
                  });
          final ImmutableList<FissClaimChange> expectedClaims =
              JsonMessageSource.parseAll(fissClaimJson, JsonMessageSource::parseFissClaimChange);
          List<RdaFissClaim> claims = getRdaFissClaims(entityManager);
          assertEquals(expectedClaims.size(), claims.size());
          for (RdaFissClaim resultClaim : claims) {
            FissClaim expected = findMatchingFissClaim(expectedClaims, resultClaim);
            assertNotNull(expected);
            assertEquals(expected.getHicNo(), resultClaim.getHicNo());
            assertEquals(
                expected.getPracLocCity(), Strings.nullToEmpty(resultClaim.getPracLocCity()));
            assertEquals(expected.getFissProcCodesCount(), resultClaim.getProcCodes().size());
            assertEquals(expected.getFissDiagCodesCount(), resultClaim.getDiagCodes().size());
          }
        });
  }

  /**
   * Verifies that an invalid FISS claim terminates the job and that all complete batches prior to
   * the bad claim have been written to the database.
   */
  @Test
  public void invalidFissClaimTest() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        RdaLoadJobIT.class,
        clock,
        (appState, entityManager) -> {
          assertTablesAreEmpty(entityManager);
          final List<String> badFissClaimJson = new ArrayList<>(fissClaimJson);
          final int badClaimIndex = badFissClaimJson.size() - 1;
          final int fullBatchSize = badFissClaimJson.size() - badFissClaimJson.size() % BATCH_SIZE;
          badFissClaimJson.set(
              badClaimIndex,
              badFissClaimJson
                  .get(badClaimIndex)
                  .replaceAll("\"hicNo\":\"\\d+\"", "\"hicNo\":\"123456789012345\""));
          RdaServer.LocalConfig.builder()
              .fissSourceFactory(fissJsonSource(badFissClaimJson))
              .build()
              .runWithPortParam(
                  port -> {
                    final RdaLoadOptions config = createRdaLoadOptions(port);
                    final RdaFissClaimLoadJob job = config.createFissClaimsLoadJob(appState);
                    try {
                      job.callRdaServiceAndStoreRecords();
                      fail("expected an exception to be thrown");
                    } catch (ProcessingException ex) {
                      assertEquals(fullBatchSize, ex.getProcessedCount());
                      assertTrue(ex.getMessage().contains("invalid length"));
                    }
                  });
          List<RdaFissClaim> claims = getRdaFissClaims(entityManager);
          assertEquals(fullBatchSize, claims.size());
        });
  }

  /**
   * All of our test claims should be valid for our IT tests to succeed. This test ensures this is
   * the case and catches any incompatibility issues when a new RDA API version contains breaking
   * changes.
   */
  @Test
  public void mcsClaimsAreValid() throws Exception {
    final ImmutableList<McsClaimChange> expectedClaims =
        JsonMessageSource.parseAll(mcsClaimJson, JsonMessageSource::parseMcsClaimChange);
    final McsClaimTransformer transformer =
        new McsClaimTransformer(clock, MbiCache.computedCache(new IdHasher.Config(1, "testing")));
    for (McsClaimChange claim : expectedClaims) {
      try {
        transformer.transformClaim(claim);
      } catch (DataTransformer.TransformationException ex) {
        fail(String.format("bad sample claim: seq=%d error=%s", claim.getSeq(), ex.getErrors()));
      }
    }
  }

  @Test
  public void mcsClaimsTest() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        RdaLoadJobIT.class,
        clock,
        (appState, entityManager) -> {
          assertTablesAreEmpty(entityManager);
          RdaServer.InProcessConfig.builder()
              .serverName(RdaServerJob.Config.DEFAULT_SERVER_NAME)
              .mcsSourceFactory(mcsJsonSource(mcsClaimJson))
              .build()
              .runWithNoParam(
                  () -> {
                    final RdaLoadOptions config = createRdaLoadOptions(-1);
                    final PipelineJob<?> job = config.createMcsClaimsLoadJob(appState);
                    job.call();
                  });
          final ImmutableList<McsClaimChange> expectedClaims =
              JsonMessageSource.parseAll(mcsClaimJson, JsonMessageSource::parseMcsClaimChange);
          List<RdaMcsClaim> claims = getRdaMcsClaims(entityManager);
          assertEquals(expectedClaims.size(), claims.size());
          for (RdaMcsClaim resultClaim : claims) {
            McsClaim expected = findMatchingMcsClaim(expectedClaims, resultClaim);
            assertNotNull(expected);
            assertEquals(expected.getIdrHic(), Strings.nullToEmpty(resultClaim.getIdrHic()));
            assertEquals(
                expected.getIdrClaimMbi(), Strings.nullToEmpty(resultClaim.getIdrClaimMbi()));
            assertEquals(expected.getMcsDetailsCount(), resultClaim.getDetails().size());
            assertEquals(expected.getMcsDiagnosisCodesCount(), resultClaim.getDiagCodes().size());
          }
        });
  }

  /**
   * Verifies that a Server error terminates the job and that all complete batches prior to the bad
   * claim have been written to the database.
   */
  @Test
  public void serverExceptionTest() throws Exception {
    RdaPipelineTestUtils.runTestWithTemporaryDb(
        RdaLoadJobIT.class,
        clock,
        (appState, entityManager) -> {
          assertTablesAreEmpty(entityManager);
          final int claimsToSendBeforeThrowing = mcsClaimJson.size() / 2;
          final int fullBatchSize =
              claimsToSendBeforeThrowing - claimsToSendBeforeThrowing % BATCH_SIZE;
          assertTrue(fullBatchSize > 0);
          RdaServer.LocalConfig.builder()
              .mcsSourceFactory(
                  ignored ->
                      new ExceptionMessageSource<>(
                          new JsonMessageSource<>(
                              mcsClaimJson, JsonMessageSource::parseMcsClaimChange),
                          claimsToSendBeforeThrowing,
                          () -> new IOException("oops")))
              .build()
              .runWithPortParam(
                  port -> {
                    final RdaLoadOptions config = createRdaLoadOptions(port);
                    final RdaMcsClaimLoadJob job = config.createMcsClaimsLoadJob(appState);
                    try {
                      job.callRdaServiceAndStoreRecords();
                      fail("expected an exception to be thrown");
                    } catch (ProcessingException ex) {
                      assertEquals(fullBatchSize, ex.getProcessedCount());
                      assertTrue(ex.getOriginalCause() instanceof StatusRuntimeException);
                    }
                  });
          List<RdaMcsClaim> claims = getRdaMcsClaims(entityManager);
          assertEquals(fullBatchSize, claims.size());
        });
  }

  @Nullable
  private FissClaim findMatchingFissClaim(
      ImmutableList<FissClaimChange> expectedClaims, RdaFissClaim resultClaim) {
    return expectedClaims.stream()
        .map(FissClaimChange::getClaim)
        .filter(claim -> claim.getDcn().equals(resultClaim.getDcn()))
        .findAny()
        .orElse(null);
  }

  @Nullable
  private McsClaim findMatchingMcsClaim(
      ImmutableList<McsClaimChange> expectedClaims, RdaMcsClaim resultClaim) {
    return expectedClaims.stream()
        .map(McsClaimChange::getClaim)
        .filter(claim -> claim.getIdrClmHdIcn().equals(resultClaim.getIdrClmHdIcn()))
        .findAny()
        .orElse(null);
  }

  private void assertTablesAreEmpty(EntityManager entityManager) throws Exception {
    assertEquals(0, getRdaFissClaims(entityManager).size());
    assertEquals(0, getRdaMcsClaims(entityManager).size());
  }

  private List<RdaMcsClaim> getRdaMcsClaims(EntityManager entityManager) {
    return entityManager
        .createQuery("select c from RdaMcsClaim c", RdaMcsClaim.class)
        .getResultList();
  }

  private List<RdaFissClaim> getRdaFissClaims(EntityManager entityManager) {
    return entityManager
        .createQuery("select c from RdaFissClaim c", RdaFissClaim.class)
        .getResultList();
  }

  private static RdaLoadOptions createRdaLoadOptions(int serverPort) {
    final RdaSourceConfig.RdaSourceConfigBuilder rdaSourceConfig = RdaSourceConfig.builder();
    if (serverPort > 0) {
      rdaSourceConfig
          .serverType(RdaSourceConfig.ServerType.Remote)
          .host("localhost")
          .port(serverPort);
    } else {
      rdaSourceConfig
          .serverType(RdaSourceConfig.ServerType.InProcess)
          .inProcessServerName(RdaServerJob.Config.DEFAULT_SERVER_NAME);
    }
    rdaSourceConfig.maxIdle(Duration.ofMinutes(1));
    rdaSourceConfig.authenticationToken("secret-token");
    return new RdaLoadOptions(
        AbstractRdaLoadJob.Config.builder()
            .runInterval(Duration.ofSeconds(1))
            .batchSize(BATCH_SIZE)
            .build(),
        rdaSourceConfig.build(),
        new RdaServerJob.Config(),
        new IdHasher.Config(100, "thisisjustatest"));
  }

  private MessageSource.Factory<FissClaimChange> fissJsonSource(List<String> claimJson) {
    // resource - This is a factory method, resource handling is done later
    //noinspection resource
    return sequenceNumber ->
        new JsonMessageSource<>(claimJson, JsonMessageSource::parseFissClaimChange)
            .skip(sequenceNumber - 1);
  }

  private MessageSource.Factory<McsClaimChange> mcsJsonSource(List<String> claimJson) {
    // resource - This is a factory method, resource handling is done later
    //noinspection resource
    return sequenceNumber ->
        new JsonMessageSource<>(claimJson, JsonMessageSource::parseMcsClaimChange)
            .skip(sequenceNumber - 1);
  }
}
