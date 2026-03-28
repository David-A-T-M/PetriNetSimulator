package ar.edu.unc.david.petrinetsimulator.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for ConfigLoader. */
class ConfigLoaderTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("load returns SimulationConfig when JSON file is valid")
  void load_returnsSimulationConfig_whenJsonIsValid() throws IOException {
    Path configPath = writeFile("valid-config.json", validConfigJson());

    SimulationConfig config = ConfigLoader.load(configPath.toString(), SimulationConfig.class);

    assertNotNull(config);
    assertNotNull(config.net());
    assertNotNull(config.placeInvariants());
    assertNotNull(config.agents());
    assertNotNull(config.logging());
    assertNotNull(config.runtime());
    assertNotNull(config.policy());

    assertEquals(2, config.net().pre().length);
    assertEquals(2, config.net().post().length);
    assertEquals(2, config.net().initialMarking().length);
    assertEquals("RANDOM", config.policy().type());
    assertEquals(5000, config.runtime().joinTimeoutMs());
    assertTrue(config.runtime().builtInValidation());
  }

  @Test
  @DisplayName("load throws RuntimeException when file does not exist")
  void load_throwsRuntimeException_whenFileDoesNotExist() {
    String missingPath = tempDir.resolve("missing.json").toString();

    RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> ConfigLoader.load(missingPath, SimulationConfig.class));

    assertTrue(ex.getMessage().contains("Failed to load"));
    assertNotNull(ex.getCause());
    assertInstanceOf(IOException.class, ex.getCause());
    assertTrue(ex.getCause().getMessage().contains("File not found"));
  }

  @Test
  @DisplayName("load throws RuntimeException when JSON is malformed")
  void load_throwsRuntimeException_whenJsonIsMalformed() throws IOException {
    Path configPath = writeFile("malformed.json", "{ this is not valid json ");

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> ConfigLoader.load(configPath.toString(), SimulationConfig.class));

    assertTrue(ex.getMessage().contains("Failed to load "));
    assertNotNull(ex.getCause());
    assertInstanceOf(IOException.class, ex.getCause());
  }

  @Test
  @DisplayName("load throws RuntimeException when JSON violates record validation")
  void load_throwsRuntimeException_whenJsonViolatesRecordValidation() throws IOException {
    // JSON sintacticamente valido, pero policy.type no soportado segun PolicyConfig.
    Path configPath =
        writeFile(
            "invalid-business-config.json", validConfigJson().replace("\"RANDOM\"", "\"FIFO\""));

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> ConfigLoader.load(configPath.toString(), SimulationConfig.class));

    assertTrue(ex.getMessage().contains("Failed to load "));
    assertNotNull(ex.getCause());
    assertInstanceOf(IOException.class, ex.getCause());
  }

  @Test
  @DisplayName("load throws RuntimeException when path points to a directory")
  void load_throwsRuntimeException_whenPathIsDirectory() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> ConfigLoader.load(tempDir.toString(), SimulationConfig.class));

    assertTrue(ex.getMessage().contains("Failed to load "));
    assertNotNull(ex.getCause());
    assertInstanceOf(IOException.class, ex.getCause());
  }

  @Test
  @DisplayName("load throws NullPointerException when path is null")
  void load_throwsNullPointerException_whenPathIsNull() {
    assertThrows(NullPointerException.class, () -> ConfigLoader.load(null, SimulationConfig.class));
  }

  private Path writeFile(String fileName, String content) throws IOException {
    Path path = tempDir.resolve(fileName);
    Files.writeString(path, content);
    return path;
  }

  private static String validConfigJson() {
    return """
        {
          "net": {
            "pre": [[1, 0], [0, 1]],
            "post": [[0, 1], [1, 0]],
            "initialMarking": [1, 0],
            "transitions": [
              { "id": 0, "alpha": 100, "beta": 200 },
              { "id": 1, "alpha": 150, "beta": 250 }
            ]
          },
          "placeInvariants": [
            { "places": [1, 1], "constant": 1 }
          ],
          "agents": [
            { "namePrefix": "P", "count": 1, "sequence": [0, 1], "cycles": 2 }
          ],
          "logging": {
            "file": "petri_log.csv",
            "logWaitWake": false
          },
          "runtime": {
            "joinTimeoutMs": 5000,
            "builtInValidation": true
          },
          "policy": {
            "type": "RANDOM"
          }
        }
        """;
  }
}
