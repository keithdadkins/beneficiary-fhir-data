package gov.cms.bfd.server.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Unit(ish) tests for {@link AppConfiguration}.
 *
 * <p>Since Java apps can't modify their environment variables at runtime, this class has a {@link
 * #main(String[])} method that the test cases will launch as an application in a separate process.
 */
public final class AppConfigurationIT {
  /**
   * Verifies that {@link AppConfiguration#readConfigFromEnvironmentVariables()} works as expected
   * when passed valid configuration environment variables.
   *
   * @throws IOException (indicates a test error)
   * @throws InterruptedException (indicates a test error)
   * @throws ClassNotFoundException (indicates a test error)
   * @throws URISyntaxException (indicates a test error)
   */
  @Test
  public void normalUsage()
      throws IOException, InterruptedException, ClassNotFoundException, URISyntaxException {
    ProcessBuilder testAppBuilder = createProcessBuilderForTestDriver();
    testAppBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_PORT, "1");
    testAppBuilder
        .environment()
        .put(
            AppConfiguration.ENV_VAR_KEY_KEYSTORE,
            getProjectDirectory()
                .resolve(Paths.get("..", "dev", "ssl-stores", "server-keystore.jks"))
                .toString());
    testAppBuilder
        .environment()
        .put(
            AppConfiguration.ENV_VAR_KEY_TRUSTSTORE,
            getProjectDirectory()
                .resolve(Paths.get("..", "dev", "ssl-stores", "server-truststore.jks"))
                .toString());
    testAppBuilder
        .environment()
        .put(
            AppConfiguration.ENV_VAR_KEY_WAR,
            getProjectDirectory()
                .resolve(
                    Paths.get("target", "sample", "bfd-server-launcher-sample-1.0.0-SNAPSHOT.war"))
                .toString());
    Process testApp = testAppBuilder.start();

    int testAppExitCode = testApp.waitFor();
    /*
     * Only pull the output if things failed, as doing so will break the deserialization happening
     * below.
     */
    String output = "";
    if (testAppExitCode != 0) output = collectOutput(testApp);
    assertEquals(0, testAppExitCode, String.format("Wrong exit code. Output[\n%s]\n", output));

    ObjectInputStream testAppOutput = new ObjectInputStream(testApp.getErrorStream());
    AppConfiguration testAppConfig = (AppConfiguration) testAppOutput.readObject();
    assertNotNull(testAppConfig);
    assertEquals(
        Integer.parseInt(testAppBuilder.environment().get(AppConfiguration.ENV_VAR_KEY_PORT)),
        testAppConfig.getPort());
    assertEquals(
        testAppBuilder.environment().get(AppConfiguration.ENV_VAR_KEY_KEYSTORE),
        testAppConfig.getKeystore().toString());
    assertEquals(
        testAppBuilder.environment().get(AppConfiguration.ENV_VAR_KEY_TRUSTSTORE),
        testAppConfig.getTruststore().toString());
    assertEquals(
        testAppBuilder.environment().get(AppConfiguration.ENV_VAR_KEY_WAR),
        testAppConfig.getWar().toString());
  }

  /**
   * Gets the project directory.
   *
   * @return the local {@link Path} to this project/module
   */
  static Path getProjectDirectory() {
    /*
     * The working directory for tests will either be the module directory or their parent
     * directory. With that knowledge, we're searching for the bluebutton-data-server-launcher
     * directory.
     */
    String projectName = "bfd-server-launcher";
    Path projectDir = Paths.get(".");
    if (!resolveRealFileName(projectDir).equals(projectName))
      projectDir = Paths.get(".", projectName);
    if (!Files.isDirectory(projectDir)) throw new IllegalStateException();
    return projectDir;
  }

  /**
   * Resolves the real file name for a {@link Path}.
   *
   * @param path the {@link Path} to resolve the real {@link Path#getFileName()} of
   * @return the real {@link Path#getFileName()} of the specified {@link Path}
   */
  static String resolveRealFileName(Path path) {
    try {
      return path.toRealPath().getFileName().toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Verifies that {@link AppConfiguration#readConfigFromEnvironmentVariables()} fails as expected
   * when it's called in an application that hasn't had any of the configuration environment
   * variables set.
   *
   * @throws IOException (indicates a test error)
   * @throws InterruptedException (indicates a test error)
   */
  @Test
  public void noEnvVarsSpecified() throws IOException, InterruptedException {
    ProcessBuilder testAppBuilder = createProcessBuilderForTestDriver();
    Process testApp = testAppBuilder.start();

    assertNotEquals(0, testApp.waitFor());
    String testAppError =
        new BufferedReader(new InputStreamReader(testApp.getErrorStream()))
            .lines()
            .collect(Collectors.joining("\n"));
    assertTrue(testAppError.contains(AppConfigurationException.class.getName()));
  }

  /**
   * Create a {@link ProcessBuilder} that will launch {@link #main(String[])} as a separate JVM
   * process.
   *
   * @return a {@link ProcessBuilder} for the main app entry
   */
  private static ProcessBuilder createProcessBuilderForTestDriver() {
    Path java = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("java");
    String classpath = System.getProperty("java.class.path");
    ProcessBuilder testAppBuilder =
        new ProcessBuilder(
            java.toAbsolutePath().toString(),
            "-classpath",
            classpath,
            AppConfigurationIT.class.getName());
    return testAppBuilder;
  }

  /**
   * Collect output from the test app's stdout.
   *
   * @param process the {@link Process} to collect the output of
   * @return the output of the specified {@link Process} in a format suitable for debugging
   */
  private static String collectOutput(Process process) {
    String stderr =
        new BufferedReader(new InputStreamReader(process.getErrorStream()))
            .lines()
            .map(l -> "\t" + l)
            .collect(Collectors.joining("\n"));
    String stdout =
        new BufferedReader(new InputStreamReader(process.getInputStream()))
            .lines()
            .map(l -> "\t" + l)
            .collect(Collectors.joining("\n"));
    return String.format("STDERR:\n[%s]\nSTDOUT:\n[%s]", stderr, stdout);
  }

  /**
   * Calls {@link AppConfiguration#readConfigFromEnvironmentVariables()} and serializes the
   * resulting {@link AppConfiguration} instance out to {@link System#err}. (Can't use {@link
   * System#out} as it might have logging noise on it.
   *
   * @param args (not used)
   */
  public static void main(String[] args) {
    AppConfiguration appConfig = AppConfiguration.readConfigFromEnvironmentVariables();

    try {
      // Serialize data object to a file
      ObjectOutputStream out = new ObjectOutputStream(System.err);
      out.writeObject(appConfig);
      out.close();
    } catch (IOException e) {
      System.out.printf("Error occurred: %s: '%s'", e.getClass().getName(), e.getMessage());
      System.exit(2);
    }
  }
}
