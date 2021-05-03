package gov.cms.bfd.pipeline.rda.grpc;

import static gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome.NOTHING_TO_DO;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import java.time.Duration;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skeleton PipelineJob instance that delegates the actual ETL work to two other objects. The
 * RDASource object handles communication with the source of incoming data. The RDASink object
 * handles communication with the ultimate storage system. The purpose of this class is to handle
 * general PipelineJob semantics that are common to any source or sink.
 */
public final class DcGeoRdaLoadJob<T> implements PipelineJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(DcGeoRdaLoadJob.class);
  public static final String SCAN_INTERVAL_PROPERTY = "DCGeoRDALoadIntervalSeconds";
  public static final int SCAN_INTERVAL_DEFAULT = 300;
  public static final String BATCH_SIZE_PROPERTY = "DCGeoBatchSize";
  public static final int BATCH_SIZE_DEFAULT = 1;
  public static final String CALLS_METER_NAME =
      MetricRegistry.name(DcGeoRdaLoadJob.class.getSimpleName(), "calls");
  public static final String FAILURES_METER_NAME =
      MetricRegistry.name(DcGeoRdaLoadJob.class.getSimpleName(), "failures");
  public static final String SUCCESSES_METER_NAME =
      MetricRegistry.name(DcGeoRdaLoadJob.class.getSimpleName(), "successes");
  public static final String PROCESSED_METER_NAME =
      MetricRegistry.name(DcGeoRdaLoadJob.class.getSimpleName(), "processed");

  private final Config config;
  private final Callable<RdaSource<T>> sourceFactory;
  private final Callable<RdaSink<T>> sinkFactory;
  private final Meter callsMeter;
  private final Meter failuresMeter;
  private final Meter successesMeter;
  private final Meter processedMeter;

  public DcGeoRdaLoadJob(
      Config config,
      Callable<RdaSource<T>> sourceFactory,
      Callable<RdaSink<T>> sinkFactory,
      MetricRegistry appMetrics) {
    this.config = config;
    this.sourceFactory = sourceFactory;
    this.sinkFactory = sinkFactory;
    callsMeter = appMetrics.meter(CALLS_METER_NAME);
    failuresMeter = appMetrics.meter(FAILURES_METER_NAME);
    successesMeter = appMetrics.meter(SUCCESSES_METER_NAME);
    processedMeter = appMetrics.meter(PROCESSED_METER_NAME);
  }

  /**
   * Factory method to construct a new job instance using standard parameters.
   *
   * @param appMetrics MetricRegistry used to track operational metrics
   * @return a DcGeoRDALoadJob instance suitable for use by PipelineManager.
   */
  public static PipelineJob newDcGeoRDALoadJob(MetricRegistry appMetrics) {
    return new DcGeoRdaLoadJob<Void>(
        new Config(),
        () -> new SkeletonRdaSource(appMetrics),
        () -> new SkeletonRdaSink(appMetrics),
        appMetrics);
  }

  public Duration getScanInterval() {
    return config.getScanInterval();
  }

  @Override
  public PipelineJobOutcome call() throws Exception {
    final long startMillis = System.currentTimeMillis();
    int processedCount = 0;
    Exception error = null;
    try {
      callsMeter.mark();
      try (RdaSource<T> source = sourceFactory.call();
          RdaSink<T> sink = sinkFactory.call()) {
        processedCount = source.retrieveAndProcessObjects(config.getBatchSize(), sink);
      }
    } catch (ProcessingException ex) {
      processedCount += ex.getProcessedCount();
      error = ex.getCause();
    } catch (Exception ex) {
      error = ex;
    }
    processedMeter.mark(processedCount);
    final long stopMillis = System.currentTimeMillis();
    LOGGER.info("processed {} objects in {} ms", processedCount, stopMillis - startMillis);
    if (error != null) {
      failuresMeter.mark();
      LOGGER.error("processing aborted by an exception: message={}", error.getMessage(), error);
      throw new ProcessingException(error, processedCount);
    }
    successesMeter.mark();
    return processedCount == 0 ? NOTHING_TO_DO : PipelineJobOutcome.WORK_DONE;
  }

  /** Immutable class containing configuration settings used by the DcGeoRDALoadJob class. */
  public static final class Config {
    private final Duration scanInterval;
    private final int batchSize;

    public Config(Duration scanInterval, int batchSize) {
      this.scanInterval = scanInterval;
      this.batchSize = batchSize;
    }

    public Config() {
      this(
          Duration.ofSeconds(ConfigUtils.getInt(SCAN_INTERVAL_PROPERTY, SCAN_INTERVAL_DEFAULT)),
          ConfigUtils.getInt(BATCH_SIZE_PROPERTY, BATCH_SIZE_DEFAULT));
    }

    public Duration getScanInterval() {
      return scanInterval;
    }

    public int getBatchSize() {
      return batchSize;
    }
  }
}
