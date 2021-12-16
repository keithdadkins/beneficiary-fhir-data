package gov.cms.bfd.server.war.r4.providers.preadj;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.server.war.commons.BBCodingSystems;
import gov.cms.bfd.server.war.commons.CommonCodings;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBIdentifierType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.codesystems.ClaimType;

/** Transforms FISS/MCS instances into FHIR {@link ClaimResponse} resources. */
public class McsClaimResponseTransformerV2 {

  private static final String METRIC_NAME =
      MetricRegistry.name(McsClaimResponseTransformerV2.class.getSimpleName(), "transform");

  private static final Map<String, Enumerations.AdministrativeGender> GENDER_MAP =
      Map.of(
          "m", Enumerations.AdministrativeGender.MALE,
          "f", Enumerations.AdministrativeGender.FEMALE,
          "u", Enumerations.AdministrativeGender.UNKNOWN);

  private static final Set<String> CANCELLED_STATUS_CODES = Set.of("r", "z", "9");

  private static final Map<String, ClaimResponse.RemittanceOutcome> OUTCOME_MAP =
      Map.ofEntries(
          Map.entry("a", ClaimResponse.RemittanceOutcome.QUEUED),
          Map.entry("b", ClaimResponse.RemittanceOutcome.QUEUED),
          Map.entry("j", ClaimResponse.RemittanceOutcome.QUEUED),
          Map.entry("k", ClaimResponse.RemittanceOutcome.QUEUED),
          Map.entry("1", ClaimResponse.RemittanceOutcome.QUEUED),
          Map.entry("2", ClaimResponse.RemittanceOutcome.QUEUED),
          Map.entry("c", ClaimResponse.RemittanceOutcome.PARTIAL),
          Map.entry("l", ClaimResponse.RemittanceOutcome.PARTIAL),
          Map.entry("3", ClaimResponse.RemittanceOutcome.PARTIAL),
          Map.entry("8", ClaimResponse.RemittanceOutcome.PARTIAL),
          Map.entry("d", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("e", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("f", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("g", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("m", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("n", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("p", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("q", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("r", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("u", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("v", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("w", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("x", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("y", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("z", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("4", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("5", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("9", ClaimResponse.RemittanceOutcome.COMPLETE));

  private McsClaimResponseTransformerV2() {}

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claimEntity the MCS {@link PreAdjMcsClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified claim
   */
  @Trace
  static ClaimResponse transform(MetricRegistry metricRegistry, Object claimEntity) {
    if (!(claimEntity instanceof PreAdjMcsClaim)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      return transformClaim((PreAdjMcsClaim) claimEntity);
    }
  }

  /**
   * @param claimGroup the {@link PreAdjMcsClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified {@link
   *     PreAdjMcsClaim}
   */
  private static ClaimResponse transformClaim(PreAdjMcsClaim claimGroup) {
    ClaimResponse claim = new ClaimResponse();

    claim.setId("m-" + claimGroup.getIdrClmHdIcn());
    claim.setContained(List.of(getContainedPatient(claimGroup)));
    claim.setExtension(getExtension(claimGroup));
    claim.setIdentifier(getIdentifier(claimGroup));
    claim.setStatus(
        CANCELLED_STATUS_CODES.contains(claimGroup.getIdrStatusCode().toLowerCase())
            ? ClaimResponse.ClaimResponseStatus.CANCELLED
            : ClaimResponse.ClaimResponseStatus.ACTIVE);
    claim.setOutcome(OUTCOME_MAP.get(claimGroup.getIdrStatusCode()));
    claim.setType(getType());
    claim.setUse(ClaimResponse.Use.CLAIM);
    claim.setInsurer(new Reference().setIdentifier(new Identifier().setValue("CMS")));
    claim.setPatient(new Reference("#patient"));
    claim.setRequest(new Reference(String.format("Claim/m-%s", claimGroup.getIdrClmHdIcn())));

    FhirContext ctx = FhirContext.forR4();
    IParser parser = ctx.newJsonParser();

    String claimContent = parser.encodeResourceToString(claim);

    String resourceHash = DigestUtils.sha1Hex(claimContent);

    claim.setMeta(
        new Meta()
            .setLastUpdated(Date.from(claimGroup.getLastUpdated()))
            .setVersionId("m-" + claimGroup.getIdrClmHdIcn() + "-" + resourceHash));
    claim.setCreated(new Date());

    return claim;
  }

  private static Resource getContainedPatient(PreAdjMcsClaim claimGroup) {
    return new Patient()
        .setIdentifier(
            List.of(
                new Identifier()
                    .setType(
                        new CodeableConcept(
                            new Coding(
                                CommonCodings.MC.getSystem(),
                                CommonCodings.MC.getCode(),
                                CommonCodings.MC.getDisplay())))
                    .setSystem(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED)
                    .setValue(claimGroup.getIdrClaimMbi())))
        .setName(getBeneName(claimGroup))
        .setGender(
            claimGroup.getIdrBeneSex() == null
                ? null
                : GENDER_MAP.get(claimGroup.getIdrBeneSex().toLowerCase()))
        .setId("patient");
  }

  private static List<HumanName> getBeneName(PreAdjMcsClaim claimGroup) {
    List<HumanName> names;

    if (claimGroup.getIdrBeneLast_1_6() != null
        || claimGroup.getIdrBeneFirstInit() != null
        || claimGroup.getIdrBeneMidInit() != null) {
      HumanName name =
          new HumanName()
              .setFamily(
                  claimGroup.getIdrBeneLast_1_6() == null
                          || claimGroup.getIdrBeneLast_1_6().isEmpty()
                      ? null
                      : claimGroup.getIdrBeneLast_1_6().charAt(0) + ".");

      if (claimGroup.getIdrBeneFirstInit() != null || claimGroup.getIdrBeneMidInit() != null) {
        name.setGiven(
            List.of(
                new StringType(
                    claimGroup.getIdrBeneFirstInit() == null
                        ? null
                        : claimGroup.getIdrBeneFirstInit() + "."),
                new StringType(
                    claimGroup.getIdrBeneMidInit() == null
                        ? null
                        : claimGroup.getIdrBeneMidInit() + ".")));
      }

      names = List.of(name);
    } else {
      names = null;
    }

    return names;
  }

  private static List<Extension> getExtension(PreAdjMcsClaim claimGroup) {
    List<Extension> extensions = new ArrayList<>();

    if (claimGroup.getIdrStatusCode() != null) {
      extensions.add(
          new Extension(BBCodingSystems.MCS.STATUS_CODE)
              .setValue(
                  new Coding(
                      BBCodingSystems.MCS.STATUS_CODE, claimGroup.getIdrStatusCode(), null)));
    }

    if (claimGroup.getIdrClaimReceiptDate() != null) {
      extensions.add(
          new Extension(BBCodingSystems.MCS.CLAIM_RECEIPT_DATE)
              .setValue(new DateType(localDateToDate(claimGroup.getIdrClaimReceiptDate()))));
    }

    if (claimGroup.getIdrStatusDate() != null) {
      extensions.add(
          new Extension(BBCodingSystems.MCS.STATUS_DATE)
              .setValue(new DateType(localDateToDate(claimGroup.getIdrStatusDate()))));
    }

    return List.copyOf(extensions);
  }

  private static List<Identifier> getIdentifier(PreAdjMcsClaim claimGroup) {
    return List.of(
        new Identifier()
            .setType(
                new CodeableConcept(
                    new Coding(
                        C4BBIdentifierType.UC.getSystem(),
                        C4BBIdentifierType.UC.toCode(),
                        C4BBIdentifierType.UC.getDisplay())))
            .setSystem(BBCodingSystems.CARR_CLM_CONTROL_NUM)
            .setValue(claimGroup.getIdrClmHdIcn()));
  }

  private static CodeableConcept getType() {
    return new CodeableConcept()
        .setCoding(
            List.of(
                new Coding(
                    ClaimType.PROFESSIONAL.getSystem(),
                    ClaimType.PROFESSIONAL.toCode(),
                    ClaimType.PROFESSIONAL.getDisplay())));
  }

  private static Date localDateToDate(LocalDate localDate) {
    return localDate == null
        ? null
        : Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }
}
