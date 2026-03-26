package ar.edu.unc.david.petrinetsimulator;

import ar.edu.unc.david.petrinetsimulator.agent.PetriAgent;
import ar.edu.unc.david.petrinetsimulator.config.AgentConfig;
import ar.edu.unc.david.petrinetsimulator.config.ConfigLoader;
import ar.edu.unc.david.petrinetsimulator.config.LoggingConfig;
import ar.edu.unc.david.petrinetsimulator.config.RuntimeConfig;
import ar.edu.unc.david.petrinetsimulator.config.SimulationConfig;
import ar.edu.unc.david.petrinetsimulator.core.PetriNet;
import ar.edu.unc.david.petrinetsimulator.core.PetriNetMatrix;
import ar.edu.unc.david.petrinetsimulator.log.PetriLogValidator;
import ar.edu.unc.david.petrinetsimulator.log.PetriLogger;
import ar.edu.unc.david.petrinetsimulator.monitor.Monitor;
import ar.edu.unc.david.petrinetsimulator.policy.Policy;
import ar.edu.unc.david.petrinetsimulator.policy.PolicyFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Main class to run the Petri net simulation for a producer-consumer scenario. */
public class Main {
  private static final int DEFAULT_JOIN_TIMEOUT_MS = 30_000;
  private static final String DEFAULT_LOG_FILE = "petri_log.csv";
  private static final String DEFAULT_CONFIG_FILE = "config.json";
  private static final int POST_SHUTDOWN_JOIN_TIMEOUT_MS = 1000;

  /** Entry point of the application. */
  public static void main(String[] args) {
    String configPath = args.length >= 1 ? args[0] : DEFAULT_CONFIG_FILE;

    SimulationConfig config = ConfigLoader.load(configPath);

    PetriNetMatrix matrix = new PetriNetMatrix(config.net().pre(), config.net().post());
    PetriNet net = new PetriNet(config.net().initialMarking(), matrix);

    LoggingConfig logging = config.logging();
    String logFile =
        (logging != null && logging.file() != null && !logging.file().isBlank())
            ? logging.file()
            : DEFAULT_LOG_FILE;

    PetriLogger logger = new PetriLogger(logFile);
    Policy policy = PolicyFactory.create(config.policy().type());
    Monitor monitor = new Monitor(net, policy, logger);

    List<Thread> threads = createAndStartAgents(config.agents(), monitor);
    int joinTimeoutMs = getJoinTimeoutMs(config.runtime());

    shutdownGracefully(threads, joinTimeoutMs);

    logger.close();

    boolean validationEnabled = config.runtime() == null || config.runtime().builtInValidation();
    if (validationEnabled) {
      validateLog(logFile);
    }
  }

  private static void validateLog(String logFile) {
    List<String> errors = PetriLogValidator.validateFile(Path.of(logFile));
    if (errors.isEmpty()) {
      System.out.println("[OK] Log is consistent.");
    } else {
      System.out.println("[WARN] Log validation errors:");
      errors.forEach(System.out::println);
    }
  }

  private static List<Thread> createAndStartAgents(List<AgentConfig> agents, Monitor monitor) {
    if (agents == null || agents.isEmpty()) {
      throw new IllegalArgumentException("Configuration must include at least one agent.");
    }

    List<Thread> threads = new ArrayList<>();
    for (AgentConfig agent : agents) {
      if (agent == null) {
        throw new IllegalArgumentException("Agent entry cannot be null.");
      }
      for (int i = 0; i < agent.count(); i++) {
        String name = agent.namePrefix() + "-" + i;
        Thread t = new Thread(new PetriAgent(monitor, agent.sequence(), agent.cycles()), name);
        threads.add(t);
        t.start();
      }
    }
    return threads;
  }

  private static int getJoinTimeoutMs(RuntimeConfig runtime) {
    if (runtime == null || runtime.joinTimeoutMs() <= 0) {
      return DEFAULT_JOIN_TIMEOUT_MS;
    }
    return runtime.joinTimeoutMs();
  }

  private static void shutdownGracefully(List<Thread> threads, int timeoutMs) {
    long deadline = System.currentTimeMillis() + timeoutMs;

    for (Thread t : threads) {
      long remaining = deadline - System.currentTimeMillis();
      if (remaining <= 0) {
        break;
      }
      try {
        t.join(remaining);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    for (Thread t : threads) {
      if (t.isAlive()) {
        t.interrupt();
      }
    }

    for (Thread t : threads) {
      try {
        t.join(POST_SHUTDOWN_JOIN_TIMEOUT_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }
}
