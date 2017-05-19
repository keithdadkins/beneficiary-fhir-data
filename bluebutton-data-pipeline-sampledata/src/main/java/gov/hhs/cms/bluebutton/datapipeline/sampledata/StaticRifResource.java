package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;

/**
 * Enumerates the sample RIF resources available on the classpath.
 */
public enum StaticRifResource {
	/**
	 * This file was manually created by copying a single beneficiary from
	 * {@link #SAMPLE_B_BENES}.
	 */
	SAMPLE_A_BENES(resourceUrl("rif-static-samples/sample-a-beneficiaries.txt"), RifFileType.BENEFICIARY, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_A_CARRIER}, and adjusting its beneficiary
	 * to match {@link #SAMPLE_A_BENES}.
	 */
	SAMPLE_A_CARRIER(resourceUrl("rif-static-samples/sample-a-bcarrier.txt"), RifFileType.CARRIER, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_A_INPATIENT}, and adjusting its
	 * beneficiary to match {@link #SAMPLE_A_INPATIENT}.
	 */
	SAMPLE_A_INPATIENT(resourceUrl("rif-static-samples/sample-a-inpatient.txt"), RifFileType.INPATIENT, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_A_OUTPATIENT}, and adjusting its
	 * beneficiary to match {@link #SAMPLE_A_OUTPATIENT}.
	 */
	SAMPLE_A_OUTPATIENT(resourceUrl("rif-static-samples/sample-a-outpatient.txt"), RifFileType.OUTPATIENT, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_A_SNF}, and adjusting its beneficiary to
	 * match {@link #SAMPLE_A_SNF}.
	 */
	SAMPLE_A_SNF(resourceUrl("rif-static-samples/sample-a-snf.txt"), RifFileType.SNF, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_A_HOSPICE}, and adjusting its beneficiary
	 * to match {@link #SAMPLE_A_HOSPICE}.
	 */
	SAMPLE_A_HOSPICE(resourceUrl("rif-static-samples/sample-a-hospice.txt"), RifFileType.HOSPICE, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_A_HHA}, and adjusting its beneficiary to
	 * match {@link #SAMPLE_A_HHA}.
	 */
	SAMPLE_A_HHA(resourceUrl("rif-static-samples/sample-a-hha.txt"), RifFileType.HHA, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_B_DME}, and adjusting its beneficiary to
	 * match {@link #SAMPLE_A_BENES}.
	 */
	SAMPLE_A_DME(resourceUrl("rif-static-samples/sample-a-dme.txt"), RifFileType.DME, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_B_CARRIER}, adjusting its beneficiary to
	 * match {@link #SAMPLE_A_BENES}, and editing some of the values to be
	 * better suited for testing against.
	 */
	SAMPLE_A_PDE(resourceUrl("rif-static-samples/sample-a-pde.txt"), RifFileType.PDE, 1),

	SAMPLE_B_BENES(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "beneficiary_test.rif"),
			RifFileType.BENEFICIARY, 100),

	/**
	 * The record count here was verified with the following shell command:
	 * <code>$ awk -F '|' '{print $4}' bluebutton-data-pipeline-sampledata/src/main/resources/rif-static-samples/sample-b-bcarrier-1440.txt | tail -n +2 | sort | uniq -c | wc -l</code>
	 * .
	 */
	SAMPLE_B_CARRIER(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "carrier_test.rif"), RifFileType.CARRIER, 3215),

	SAMPLE_B_INPATIENT(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "inpatient_test.rif"),
			RifFileType.INPATIENT, 41),

	SAMPLE_B_OUTPATIENT(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "outpatient_test.rif"),
			RifFileType.OUTPATIENT, 516),

	SAMPLE_B_SNF(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "snf_test.rif"), RifFileType.SNF, 23),

	SAMPLE_B_HOSPICE(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "hospice_test.rif"), RifFileType.HOSPICE,
			5),

	SAMPLE_B_HHA(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "hha_test.rif"), RifFileType.HHA, 32),

	SAMPLE_B_DME(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "dme_test.rif"), RifFileType.DME, 244),

	SAMPLE_B_PDE(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "pde_test.rif"), RifFileType.PDE, 4793),

	/**
	 * The record count here was verified with the following shell command:
	 * <code>$ tar --bzip2 --extract --to-stdout --file bluebutton-data-pipeline-sampledata/src/main/resources/rif-static-samples/sample-c.tar.bz2 bcarrier_sample.txt | awk -F '|' '{print $3}' | tail -n +2 | sort | uniq -c | wc -l</code>
	 * .
	 */
	SAMPLE_C_BENES(zippedResourceUrl("rif-static-samples/sample-c.tar.bz2", "bene_sample.txt"), RifFileType.BENEFICIARY, 999999),

	/**
	 * The record count here was verified with the following shell command:
	 * <code>$ tar --bzip2 --extract --to-stdout --file bluebutton-data-pipeline-sampledata/src/main/resources/rif-static-samples/sample-c.tar.bz2 bene_sample.txt | awk -F '|' '{print $4}' | tail -n +2 | sort | uniq -c | wc -l</code>
	 * .
	 */
	SAMPLE_C_CARRIER(zippedResourceUrl("rif-static-samples/sample-c.tar.bz2", "bcarrier_sample.txt"),
			RifFileType.CARRIER, 423999),

	/**
	 * The record count here was verified with the following shell command:
	 * <code>$ tar --bzip2 --extract --to-stdout --file bluebutton-data-pipeline-sampledata/src/main/resources/rif-static-samples/sample-c.tar.bz2 dme_sample.txt | awk -F '|' '{print $4}' | tail -n +2 | sort | uniq -c | wc -l</code>
	 * .
	 */
	SAMPLE_C_DME(zippedResourceUrl("rif-static-samples/sample-c.tar.bz2", "dme_sample.txt"), RifFileType.DME, 103404),

	/**
	 * The record count here was verified with the following shell command:
	 * <code>$ tar --bzip2 --extract --to-stdout --file bluebutton-data-pipeline-sampledata/src/main/resources/rif-static-samples/sample-c.tar.bz2 hha_sample.txt | awk -F '|' '{print $4}' | tail -n +2 | sort | uniq -c | wc -l</code>
	 * .
	 */
	SAMPLE_C_HHA(zippedResourceUrl("rif-static-samples/sample-c.tar.bz2", "hha_sample.txt"), RifFileType.HHA, 27800),

	/**
	 * The record count here was verified with the following shell command:
	 * <code>$ tar --bzip2 --extract --to-stdout --file bluebutton-data-pipeline-sampledata/src/main/resources/rif-static-samples/sample-c.tar.bz2 hospice_sample.txt | awk -F '|' '{print $4}' | tail -n +2 | sort | uniq -c | wc -l</code>
	 * .
	 */
	SAMPLE_C_HOSPICE(zippedResourceUrl("rif-static-samples/sample-c.tar.bz2", "hospice_sample.txt"),
			RifFileType.HOSPICE, 9205),

	/**
	 * The record count here was verified with the following shell command:
	 * <code>$ tar --bzip2 --extract --to-stdout --file bluebutton-data-pipeline-sampledata/src/main/resources/rif-static-samples/sample-c.tar.bz2 inpatient_sample.txt | awk -F '|' '{print $4}' | tail -n +2 | sort | uniq -c | wc -l</code>
	 * .
	 */
	SAMPLE_C_INPATIENT(zippedResourceUrl("rif-static-samples/sample-c.tar.bz2", "inpatient_sample.txt"),
			RifFileType.INPATIENT, 28226),

	/**
	 * The record count here was verified with the following shell command:
	 * <code>$ tar --bzip2 --extract --to-stdout --file bluebutton-data-pipeline-sampledata/src/main/resources/rif-static-samples/sample-c.tar.bz2 outpatient_sample.txt | awk -F '|' '{print $4}' | tail -n +2 | sort | uniq -c | wc -l</code>
	 * .
	 */
	SAMPLE_C_OUTPATIENT(zippedResourceUrl("rif-static-samples/sample-c.tar.bz2", "outpatient_sample.txt"),
			RifFileType.OUTPATIENT, 278036),

	/**
	 * The record count here was verified with the following shell command:
	 * <code>$ tar --bzip2 --extract --to-stdout --file bluebutton-data-pipeline-sampledata/src/main/resources/rif-static-samples/sample-c.tar.bz2 pde_sample.txt | awk -F '|' '{print $3}' | tail -n +2 | sort | uniq -c | wc -l</code>
	 * .
	 */
	SAMPLE_C_PDE(zippedResourceUrl("rif-static-samples/sample-c.tar.bz2", "pde_sample.txt"), RifFileType.PDE, 1195),

	/**
	 * The record count here was verified with the following shell command:
	 * <code>$ tar --bzip2 --extract --to-stdout --file bluebutton-data-pipeline-sampledata/src/main/resources/rif-static-samples/sample-c.tar.bz2 snf_sample.txt | awk -F '|' '{print $7}' | tail -n +2 | sort | uniq -c | wc -l</code>
	 * .
	 */
	SAMPLE_C_SNF(zippedResourceUrl("rif-static-samples/sample-c.tar.bz2", "snf_sample.txt"), RifFileType.SNF, 596);

	private final Supplier<URL> resourceUrlSupplier;
	private final RifFileType rifFileType;
	private final int recordCount;

	private URL resourceUrl;

	/**
	 * Enum constant constructor.
	 * 
	 * @param resourceUrlSupplier
	 *            the value to use for {@link #getResourceSupplier()}
	 * @param rifFileType
	 *            the value to use for {@link #getRifFileType()}
	 * @param recordCount
	 *            the value to use for {@link #getRecordCount()}
	 */
	private StaticRifResource(Supplier<URL> resourceUrlSupplier, RifFileType rifFileType, int recordCount) {
		this.resourceUrlSupplier = resourceUrlSupplier;
		this.rifFileType = rifFileType;
		this.recordCount = recordCount;
	}

	/**
	 * @return the {@link URL} to the resource's contents
	 */
	public synchronized URL getResourceUrl() {
		if (resourceUrl == null)
			resourceUrl = resourceUrlSupplier.get();

		return resourceUrl;
	}

	/**
	 * @return the {@link RifFileType} of the RIF file
	 */
	public RifFileType getRifFileType() {
		return rifFileType;
	}

	/**
	 * @return the number of beneficiaries/claims/drug events in the RIF file
	 */
	public int getRecordCount() {
		return recordCount;
	}

	/**
	 * @return a {@link RifFile} based on this {@link StaticRifResource}
	 */
	public RifFile toRifFile() {
		return new StaticRifFile(this);
	}

	/**
	 * @param resourceName
	 *            the name of the resource on the classpath (as might be passed
	 *            to {@link ClassLoader#getResource(String)})
	 * @return a {@link Supplier} for the {@link URL} to the resource's contents
	 */
	private static Supplier<URL> resourceUrl(String resourceName) {
		return () -> {
			URL resource = Thread.currentThread().getContextClassLoader().getResource(resourceName);
			if (resource == null)
				throw new IllegalArgumentException("Unable to find resource: " + resourceName);

			return resource;
		};
	}

	/**
	 * @param archiveName
	 *            the classpath/resource name of the archive file that the
	 *            desired resource is contained in (as might be passed to
	 *            {@link ClassLoader#getResource(String)})
	 * @param resourceFile
	 *            the path of the desired resource file within the specified
	 *            archive
	 * @return a {@link Supplier} for the {@link URL} to the resource's contents
	 */
	private synchronized static Supplier<URL> zippedResourceUrl(String archiveName, String resourceFile) {
		
		/*
		 * Temporarily turning this into a no-op, to prevent test and compile errors. Should be un-commented-out once sample data is available again.
		 */
		return null;
		
//		// Find the build output directory to decompress to.
//		Path targetDir = Paths.get(".", "bluebutton-data-pipeline-sampledata", "target");
//		if (!Files.exists(targetDir))
//			targetDir = Paths.get("..", "bluebutton-data-pipeline-sampledata", "target");
//		if (!Files.exists(targetDir))
//			throw new IllegalStateException();
//
//		// Check to see if it's already decompressed, and return it if so.
//		Path decompressDir = targetDir.resolve(archiveName);
//		Path resourcePath = decompressDir.resolve(resourceFile);
//		Supplier<URL> resourceUrlSupplier = () -> {
//			try {
//				return resourcePath.toUri().toURL();
//			} catch (MalformedURLException e) {
//				throw new BadCodeMonkeyException();
//			}
//		};
//		if (Files.isReadable(resourcePath))
//			return resourceUrlSupplier;
//
//		// Decompress the entire archive.
//		try (InputStream archiveStream = Thread.currentThread().getContextClassLoader()
//				.getResourceAsStream(archiveName);
//				BufferedInputStream bufferedStream = new BufferedInputStream(archiveStream);
//				BZip2CompressorInputStream bzip2Stream = new BZip2CompressorInputStream(bufferedStream);
//				TarArchiveInputStream tarStream = new TarArchiveInputStream(bzip2Stream);) {
//			TarArchiveEntry entry = null;
//			while ((entry = tarStream.getNextTarEntry()) != null) {
//				if (!entry.isFile())
//					throw new IllegalArgumentException(
//							String.format("Unexpected entry '{}' in archive '{}'.", entry.getName(), archiveName));
//				File entryFile = decompressDir.resolve(entry.getName()).toFile();
//				Files.createDirectories(entryFile.toPath().getParent());
//				try (FileOutputStream entryStream = new FileOutputStream(entryFile)) {
//					IOUtils.copy(tarStream, entryStream);
//				}
//			}
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
//
//		// Verify that the expected file/entry was decompressed and return it.
//		if (!Files.isReadable(resourcePath))
//			throw new BadCodeMonkeyException(
//					String.format("Didn't find resource '{}' in archive '{}'.", resourceFile, archiveName));
//		return resourceUrlSupplier;
	}

	/**
	 * @param dataSetLocation
	 *            the {@link TestDataSetLocation} of the file to get a local
	 *            copy of
	 * @param fileName
	 *            the name of the specific file in the specified
	 *            {@link TestDataSetLocation} to get a local copy of, e.g.
	 *            "beneficiaries.rif"
	 * @return a {@link URL} to a local copy of the specified test data file
	 *         from S3
	 */
	private static Supplier<URL> localCopyOfS3Data(TestDataSetLocation dataSetLocation, String fileName) {
		return () -> {
			// Find the build output `target` directory.
			Path targetDir = Paths.get(".", "bluebutton-data-pipeline-sampledata", "target");
			if (!Files.exists(targetDir))
				targetDir = Paths.get("..", "bluebutton-data-pipeline-sampledata", "target");
			if (!Files.exists(targetDir))
				throw new IllegalStateException();

			// Build the path that the resources will be downloaded to.
			Path resourceDir = targetDir.resolve("test-data-from-s3").resolve(dataSetLocation.getS3BucketName())
					.resolve(dataSetLocation.getS3KeyPrefix());
			Path resourceLocalCopy = resourceDir.resolve(fileName);

			/*
			 * Implementation note: we have to carefully leverage
			 * synchronization to ensure that we don't end up with multiple
			 * copies of the same file. To avoid pegging dev systems, it's also
			 * best to ensure that we're only grabbing one file at a time.
			 * Locking on the static class object accomplishes these goals.
			 */
			synchronized (StaticRifResource.class) {
				// Ensure the directory exists.
				if (!Files.exists(resourceDir)) {
					try {
						Files.createDirectories(resourceDir);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}

				// Download the file, if needed.
				if (!Files.exists(resourceLocalCopy)) {
					downloadFromS3(dataSetLocation, fileName, resourceLocalCopy);
				}
			}

			// We now know the file exists, so return it.
			try {
				return resourceLocalCopy.toUri().toURL();
			} catch (MalformedURLException e) {
				throw new BadCodeMonkeyException(e);
			}
		};
	}

	/**
	 * Downloads the specified S3 object to the specified local path.
	 * 
	 * @param dataSetLocation
	 *            the {@link TestDataSetLocation} of the S3 object to download
	 * @param fileName
	 *            the name of the specific object/file to be downloaded
	 * @param downloadPath
	 *            the {@link Path} to download the S3 object to
	 */
	private static final void downloadFromS3(TestDataSetLocation dataSetLocation, String fileName, Path downloadPath) {
		/*
		 * To avoid dragging in the S3 client libraries, we require here that
		 * the test data files be available publicly via HTTP.
		 */

		FileOutputStream outputStream = null;
		try {
			URL s3DownloadUrl = new URL(String.format("http://%s.s3.amazonaws.com/%s/%s",
					dataSetLocation.getS3BucketName(), dataSetLocation.getS3KeyPrefix(), fileName));
			ReadableByteChannel channel = Channels.newChannel(s3DownloadUrl.openStream());
			outputStream = new FileOutputStream(downloadPath.toFile());
			outputStream.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
		} catch (MalformedURLException e) {
			throw new BadCodeMonkeyException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}
	}
}
