package gov.cms.bfd.pipeline.ccw.rif.load;

import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.io.Serializable;

/** Models the user-configurable application options. */
public final class LoadAppOptions implements Serializable {
  /*
   * This class is marked Serializable purely to help keep
   * AppConfigurationTest simple. Unfortunately, Path implementations aren't
   * also Serializable, so we have to store Strings here, instead.
   */

  private static final long serialVersionUID = 2884121140016566847L;

  /**
   * A reasonable (though not terribly performant) suggested default value for {@link
   * #getLoaderThreads()}.
   */
  public static final int DEFAULT_LOADER_THREADS =
      Math.max(1, (Runtime.getRuntime().availableProcessors() - 1)) * 2;

  private final IdHasher.Config idHasherConfig;
  private final int loaderThreads;
  private final boolean idempotencyRequired;
  private final boolean filteringNonNullAndNon2023Benes;

  /**
   * The number of {@link RifRecordEvent}s that will be included in each processing batch. Note that
   * larger batch sizes mean that more {@link RifRecordEvent}s will be held in memory
   * simultaneously.
   */
  private final int recordBatchSize;

  /**
   * Constructs a new {@link LoadAppOptions} instance.
   *
   * @param idHasherConfig the value to use for {@link #getIdHasherConfig()}
   * @param loaderThreads the value to use for {@link #getLoaderThreads()}
   * @param idempotencyRequired the value to use for {@link #isIdempotencyRequired()}
   * @param filterNon2023Benes the filter non 2023 benes
   * @param recordBatchSize the load batch size
   */
  public LoadAppOptions(
      IdHasher.Config idHasherConfig,
      int loaderThreads,
      boolean idempotencyRequired,
      boolean filterNon2023Benes,
      int recordBatchSize) {
    if (loaderThreads < 1) throw new IllegalArgumentException();

    this.idHasherConfig = idHasherConfig;
    this.loaderThreads = loaderThreads;
    this.idempotencyRequired = idempotencyRequired;
    this.filteringNonNullAndNon2023Benes = filterNon2023Benes;
    this.recordBatchSize = recordBatchSize;
  }

  /** @return the configuration settings used when hashing beneficiary HICNs */
  public IdHasher.Config getIdHasherConfig() {
    return idHasherConfig;
  }

  /**
   * @return the number of threads that will be used to simultaneously process {@link RifLoader}
   *     operations
   */
  public int getLoaderThreads() {
    return loaderThreads;
  }

  /**
   * Gets the number of {@link RifRecordEvent}s that will be included in each processing batch.
   *
   * @return the batch size
   */
  public int getRecordBatchSize() {
    return recordBatchSize;
  }

  /**
   * @return
   *     <p><code>true</code> if {@link RifLoader} should check to see if each record has already
   *     been processed, <code>false</code> if it should blindly assume that it hasn't
   *     <p>This is sometimes a reasonable speed vs. safety tradeoff to make, as that checking is
   *     slow, particularly if indexes have been dropped in an attempt to speed up initial loads.
   *     Aside from that, though, this value is best left set to <code>true</code>.
   */
  public boolean isIdempotencyRequired() {
    return idempotencyRequired;
  }

  /**
   * Gets if the filtering for non-null and 2023 benes is active or not.
   *
   * <p>As part of <a href="https://jira.cms.gov/browse/BFD-1566">BFD-1566</a> and <a
   * href="https://jira.cms.gov/browse/BFD-2265">BFD-2265</a>, we want a filtering mechanism in our
   * loads such some {@link Beneficiary}s are temporarily skipped: only those with a {@link
   * Beneficiary#getBeneEnrollmentReferenceYear()} of "2023" or where the reference year is <code>
   * null</code> will be processed (which, during the calendar year of 2023, is most of them). As
   * part of this filtering, we are implementing an assumption that no non-2023 <code>INSERT
   * </code> {@link Beneficiary} records will be received, as skipping those would also require
   * skipping their associated claims, which is additional complexity that we want to avoid. If any
   * such records are encountered, the load will go boom. This filtering is an inelegant hack to
   * workaround upstream data issues, and will hopefully only be in place very temporarily. See the
   * code that uses this field in {@link RifLoader} for details. This filtering is being made
   * configurable so as to not invalidate all of our existing test coverage.
   *
   * @return filtering is enabled when this field is <code>true</code>, and disabled when it's
   *     <code>false</code>
   */
  public boolean isFilteringNonNullAndNon2023Benes() {
    return filteringNonNullAndNon2023Benes;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("LoadAppOptions [hicnHashIterations=");
    builder.append(idHasherConfig.getHashIterations());
    builder.append(", hicnHashPepper=");
    builder.append("***");
    builder.append(", loaderThreads=");
    builder.append(loaderThreads);
    builder.append(", idempotencyRequired=");
    builder.append(idempotencyRequired);
    builder.append(", filteringNonNullAndNon2023Benes=");
    builder.append(filteringNonNullAndNon2023Benes);
    builder.append("]");
    return builder.toString();
  }
}
