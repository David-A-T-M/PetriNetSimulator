package ar.edu.unc.david.petrinetsimulator.runner;

import ar.edu.unc.david.petrinetsimulator.config.ConfigLoader;
import ar.edu.unc.david.petrinetsimulator.config.SimulationConfig;
import ar.edu.unc.david.petrinetsimulator.core.SimulatorEngine;
import ar.edu.unc.david.petrinetsimulator.log.PetriLogger;
import java.util.List;

/** Console-based runner: executes the simulation without any UI. */
public class ConsoleRunner {
  /** Runs the simulation based on the provided configuration file. */
  public static void main(String[] args) {
    String configPath = args.length >= 1 ? args[0] : "config.json";
    SimulationConfig config = ConfigLoader.load(configPath, SimulationConfig.class);

    PetriLogger logger = new PetriLogger(SimulatorEngine.resolveLogFile(config.logging()));
    SimulatorEngine.Components c = SimulatorEngine.build(config, logger);

    List<Thread> threads = SimulatorEngine.startAgents(c.agents(), c.monitor());
    SimulatorEngine.shutdownGracefully(threads, c.runtime().joinTimeoutMs());

    logger.close();

    if (c.runtime().builtInValidation()) {
      SimulatorEngine.validateLog(c.logFile());
    }
  }
}
