package ar.edu.unc.david.petrinetsimulator.core;

import ar.edu.unc.david.petrinetsimulator.agent.PetriAgent;
import ar.edu.unc.david.petrinetsimulator.config.AgentConfig;
import ar.edu.unc.david.petrinetsimulator.config.LoggingConfig;
import ar.edu.unc.david.petrinetsimulator.config.NetConfig;
import ar.edu.unc.david.petrinetsimulator.config.RuntimeConfig;
import ar.edu.unc.david.petrinetsimulator.config.SimulationConfig;
import ar.edu.unc.david.petrinetsimulator.config.TransitionConfig;
import ar.edu.unc.david.petrinetsimulator.log.PetriLogValidator;
import ar.edu.unc.david.petrinetsimulator.log.PetriLogger;
import ar.edu.unc.david.petrinetsimulator.monitor.Monitor;
import ar.edu.unc.david.petrinetsimulator.policy.Policy;
import ar.edu.unc.david.petrinetsimulator.policy.PolicyFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Core engine: responsible for building the Petri net, starting agents, and managing shutdown. */
public class SimulatorEngine {

  /** Immutable container for all components needed to run the simulation. */
  public record Components(
      PetriNet net,
      Monitor monitor,
      List<AgentConfig> agents,
      RuntimeConfig runtime,
      String logFile) {}

  /** Builds the Petri net and monitor based on the provided simulation configuration. */
  public static Components build(SimulationConfig config, PetriLogger logger) {
    NetConfig net = config.net();
    PetriNetMatrix matrix = new PetriNetMatrix(net.pre(), net.post());
    long[] alpha = buildAlphaArray(net, matrix.numTransitions());
    long[] beta = buildBetaArray(net, matrix.numTransitions());
    PetriNet petriNet = new PetriNet(net.initialMarking(), matrix, alpha, beta);

    Policy policy = PolicyFactory.create(config.policy().type());
    Monitor monitor = new Monitor(petriNet, policy, logger);

    return new Components(
        petriNet, monitor, config.agents(), config.runtime(), resolveLogFile(config.logging()));
  }

  /** Starts a thread for each agent based on the provided agent configurations and monitor. */
  public static List<Thread> startAgents(List<AgentConfig> agents, Monitor monitor) {
    List<Thread> threads = new ArrayList<>();
    for (AgentConfig agent : agents) {
      for (int i = 0; i < agent.count(); i++) {
        String name = agent.namePrefix() + "-" + i;
        Thread t = new Thread(new PetriAgent(monitor, agent.sequence(), agent.cycles()), name);
        threads.add(t);
        t.start();
      }
    }
    return threads;
  }

  /** Attempts to gracefully shut down all agent threads within the specified timeout. */
  public static void shutdownGracefully(List<Thread> threads, int timeoutMs) {
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
        t.join(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  /** Validates the log file using the PetriLogValidator and prints any errors found. */
  public static void validateLog(String logFile) {
    List<String> errors = PetriLogValidator.validateFile(Path.of(logFile));
    if (errors.isEmpty()) {
      System.out.println("[OK] Log is consistent.");
    } else {
      System.out.println("[WARN] Log validation errors:");
      errors.forEach(System.out::println);
    }
  }

  /**
   * Resolves the log file name from the logging configuration, defaulting to "petri_log.csv" if not
   * specified.
   */
  public static String resolveLogFile(LoggingConfig logging) {
    if (logging != null && logging.file() != null && !logging.file().isBlank()) {
      return logging.file();
    }
    return "petri_log.csv";
  }

  private static long[] buildAlphaArray(NetConfig net, int numTransitions) {
    long[] alpha = new long[numTransitions];
    for (TransitionConfig t : net.transitions()) {
      alpha[t.id()] = t.alpha();
    }
    return alpha;
  }

  private static long[] buildBetaArray(NetConfig net, int numTransitions) {
    long[] beta = new long[numTransitions];
    for (TransitionConfig t : net.transitions()) {
      beta[t.id()] = t.beta();
    }
    return beta;
  }
}
