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
import ar.edu.unc.david.petrinetsimulator.policy.Policy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Integration test that proves policy is consulted before an enabled transition barges. */
class MonitorPolicyPrecedenceTest {

  private static final Pattern FIRE_LINE = Pattern.compile("^\\d{13},.*");

  @TempDir Path tempDir;

  @Test
  @DisplayName(
      "waiting candidate fires before a newly arrived enabled transition when policy prefers it")
  void waitingCandidateFiresBeforeNewlyArrivedEnabledTransition() throws Exception {
    Path logFile = tempDir.resolve("policy-precedence.log");
    PetriLogger logger = new PetriLogger(logFile.toString());

    try {
      assertTimeoutPreemptively(
          Duration.ofSeconds(5),
          () -> {
            SimulationConfig config =
                new SimulationConfig(
                    new NetConfig(
                        new int[][] {{1, 1}},
                        new int[][] {{1, 1}},
                        new int[] {1},
                        List.of(
                            new TransitionConfig(0, 0L, 999_999L),
                            new TransitionConfig(1, 120L, 999_999L))),
                    List.of(),
                    List.of(
                        new AgentConfig("B", 1, new int[] {1}, 1, null),
                        new AgentConfig("A1", 1, new int[] {0}, 1, null),
                        new AgentConfig("A2", 1, new int[] {0}, 1, null)),
                    new LoggingConfig(logFile.toString(), false),
                    new RuntimeConfig(5_000, true),
                    new PolicyConfig("RANDOM", List.of()),
                    new StopConfig(0, 3),
                    List.of());
            ScriptedPolicy policy = new ScriptedPolicy(0, 1, 0);
            SimulatorEngine.Components components = SimulatorEngine.build(config, logger, policy);

            List<Thread> initialThreads =
                SimulatorEngine.startAgents(
                    components.agents().subList(0, 2), components.monitor());

            Thread.sleep(250L);

            List<Thread> threads = new java.util.ArrayList<>(initialThreads);
            threads.addAll(
                SimulatorEngine.startAgents(
                    components.agents().subList(2, 3), components.monitor()));

            for (Thread thread : threads) {
              thread.join(2_000);
            }

            assertTrue(threads.stream().noneMatch(Thread::isAlive));
          });
    } finally {
      logger.close();
    }

    List<String> fireLines =
        Files.readAllLines(logFile).stream()
            .filter(line -> FIRE_LINE.matcher(line).matches())
            .toList();

    assertEquals(3, fireLines.size());
    assertTrue(fireLines.get(0).contains(",0,"));
    assertTrue(fireLines.get(1).contains(",1,"));
    assertTrue(fireLines.get(2).contains(",0,"));
  }

  @Test
  @DisplayName("proportion policy integration picks waiting candidate before barging thread")
  void proportionPolicyPicksWaitingCandidateBeforeBargingThread() throws Exception {
    Path logFile = tempDir.resolve("policy-proportion-integration.log");
    PetriLogger logger = new PetriLogger(logFile.toString());

    try {
      assertTimeoutPreemptively(
          Duration.ofSeconds(5),
          () -> {
            SimulationConfig config =
                new SimulationConfig(
                    new NetConfig(
                        new int[][] {{1, 1}},
                        new int[][] {{1, 1}},
                        new int[] {1},
                        List.of(
                            new TransitionConfig(0, 0L, 999_999L),
                            new TransitionConfig(1, 120L, 999_999L))),
                    List.of(),
                    List.of(
                        new AgentConfig("B", 1, new int[] {1}, 1, null),
                        new AgentConfig("A1", 1, new int[] {0}, 1, null),
                        new AgentConfig("A2", 1, new int[] {0}, 1, null)),
                    new LoggingConfig(logFile.toString(), false),
                    new RuntimeConfig(5_000, true),
                    new PolicyConfig(
                        "PROPORTION",
                        List.of(new PolicyGroupConfig(new int[] {0, 1}, new int[] {1, 1}))),
                    new StopConfig(0, 3),
                    List.of());

            SimulatorEngine.Components components = SimulatorEngine.build(config, logger);

            List<Thread> initialThreads =
                SimulatorEngine.startAgents(
                    components.agents().subList(0, 2), components.monitor());

            Thread.sleep(250L);

            List<Thread> threads = new java.util.ArrayList<>(initialThreads);
            threads.addAll(
                SimulatorEngine.startAgents(
                    components.agents().subList(2, 3), components.monitor()));

            for (Thread thread : threads) {
              thread.join(2_000);
            }

            assertTrue(threads.stream().noneMatch(Thread::isAlive));
          });
    } finally {
      logger.close();
    }

    List<String> fireLines =
        Files.readAllLines(logFile).stream()
            .filter(line -> FIRE_LINE.matcher(line).matches())
            .toList();

    assertEquals(3, fireLines.size());
    assertTrue(fireLines.get(0).contains(",0,"));
    assertTrue(fireLines.get(1).contains(",1,"));
    assertTrue(fireLines.get(2).contains(",0,"));
  }

  private static final class ScriptedPolicy implements Policy {
    private final int[] script;
    private int cursor;

    private ScriptedPolicy(int... script) {
      this.script = script.clone();
    }

    @Override
    public synchronized int decide(int[] candidates) {
      int index = cursor++;
      int desired = script[Math.min(index, script.length - 1)];

      for (int candidate : candidates) {
        if (candidate == desired) {
          return candidate;
        }
      }

      return candidates[0];
    }
  }
}
