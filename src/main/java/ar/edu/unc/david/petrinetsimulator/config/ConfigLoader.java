package ar.edu.unc.david.petrinetsimulator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

/** Utility class to load the simulation configuration from a JSON file. */
public class ConfigLoader {

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Loads the simulation configuration from the specified JSON file path.
   *
   * @param path the file path to the JSON configuration file
   * @return the loaded SimulationConfig object
   * @throws RuntimeException if there is an error reading or parsing the configuration file
   * @throws NullPointerException if the path is null
   * @throws SecurityException if there are security restrictions preventing file access
   */
  public static SimulationConfig load(String path) {
    try {
      File configFile = new File(path);
      if (!configFile.exists()) {
        throw new IOException("File not found: " + path);
      }

      return mapper.readValue(configFile, SimulationConfig.class);

    } catch (IOException e) {
      throw new RuntimeException("Failed to load configuration from " + path, e);
    }
  }
}
