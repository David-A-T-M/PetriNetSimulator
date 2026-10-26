package ar.edu.unc.david.petrinetsimulator.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.unc.david.petrinetsimulator.config.logic.AgentConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.LoggingConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.NetConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.PolicyConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.PolicyGroupConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.RuntimeConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.SimulationConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.StopConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.TransitionConfig;
import ar.edu.unc.david.petrinetsimulator.core.SimulatorEngine;
import ar.edu.unc.david.petrinetsimulator.log.PetriLogger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Integration tests for policy fidelity under contention. */
class MonitorPolicyFidelityTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("proportion policy approximates 1:3 ratio on real firings under contention")
  void proportionPolicyApproximatesConfiguredRatio() throws Exception {
    Path logFile = tempDir.resolve("fidelity-proportion.log");
    SimulationConfig config = proportionConfig(logFile, "PROPORTION");

    run(config, logFile);

    int t0 = countTransition(logFile, 0);
    int t1 = countTransition(logFile, 1);

    assertEquals(400, t0 + t1);
    assertTrue(t1 >= 270 && t1 <= 330, "T1 out of expected range: " + t1);
  }

  @Test
  @DisplayName("random policy remains within broad stochastic range under contention")
  void randomPolicyStaysWithinBroadRange() throws Exception {
    Path logFile = tempDir.resolve("fidelity-random.log");
    SimulationConfig config = proportionConfig(logFile, "RANDOM");

    run(config, logFile);

    int t0 = countTransition(logFile, 0);
    int t1 = countTransition(logFile, 1);

    assertEquals(400, t0 + t1);
    assertTrue(t1 >= 160 && t1 <= 240, "T1 out of expected range: " + t1);
  }

  private static SimulationConfig proportionConfig(Path logFile, String policyType) {
    int[][] pre = {
      {1, 1, 0, 0, 0},
      {1, 1, 0, 0, 0},
      {0, 0, 0, 0, 1},
      {0, 0, 1, 0, 0},
      {0, 0, 0, 1, 0}
    };

    int[][] post = {
      {0, 0, 1, 1, 0},
      {0, 0, 0, 0, 0},
      {0, 0, 1, 1, 0},
      {1, 0, 0, 0, 0},
      {0, 1, 0, 0, 0}
    };

    PolicyConfig policy =
        "PROPORTION".equals(policyType)
            ? new PolicyConfig(
                "PROPORTION", List.of(new PolicyGroupConfig(new int[] {0, 1}, new int[] {1, 3})))
            : new PolicyConfig("RANDOM", List.of());

    return new SimulationConfig(
        new NetConfig(
            pre,
            post,
            new int[] {1, 400, 0, 0, 0},
            List.of(
                new TransitionConfig(0, 0L, 999_999L),
                new TransitionConfig(1, 0L, 999_999L),
                new TransitionConfig(2, 0L, 999_999L),
                new TransitionConfig(3, 0L, 999_999L),
                new TransitionConfig(4, 0L, 999_999L))),
        List.of(),
        List.of(
            new AgentConfig("A", 2, new int[] {0, 2}, -1, null),
            new AgentConfig("B", 2, new int[] {1, 3}, -1, null),
            new AgentConfig("E", 1, new int[] {4}, -1, null)),
        new LoggingConfig(logFile.toString(), false),
        new RuntimeConfig(20_000, true),
        policy,
        new StopConfig(4, 400),
        List.of());
  }

  private static void run(SimulationConfig config, Path logFile) throws Exception {
    PetriLogger logger = new PetriLogger(logFile.toString());
    try {
      assertTimeoutPreemptively(
          Duration.ofSeconds(20),
          () -> {
            SimulatorEngine.Components components = SimulatorEngine.build(config, logger);
            List<Thread> threads =
                SimulatorEngine.startAgents(components.agents(), components.monitor());
            SimulatorEngine.shutdownGracefully(threads, components.runtime().joinTimeoutMs());
            assertTrue(threads.stream().noneMatch(Thread::isAlive));
          });
    } finally {
      logger.close();
    }
  }

  private static int countTransition(Path logFile, int transitionId) throws Exception {
    Pattern pattern = Pattern.compile("^\\d{13},[^,]*,(\\d+),.*$");
    int count = 0;
    for (String line : Files.readAllLines(logFile)) {
      Matcher matcher = pattern.matcher(line);
      if (matcher.matches() && Integer.parseInt(matcher.group(1)) == transitionId) {
        count++;
      }
    }
    return count;
  }
}
