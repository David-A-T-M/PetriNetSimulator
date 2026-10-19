package ar.edu.unc.david.petrinetsimulator.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.unc.david.petrinetsimulator.config.logic.AgentConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.LoggingConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.NetConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.PolicyConfig;
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
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Integration tests for the global stop condition wiring. */
public class MonitorStopConditionTest {

  private static final Pattern FIRE_LINE = Pattern.compile("^\\d{13},.*");

  @TempDir Path tempDir;

  @Test
  @DisplayName("simulation stops at the configured transition count and wakes waiting threads")
  void simulationStopsAtConfiguredTransitionCountAndWakesWaitingThreads() throws Exception {
    Path logFile = tempDir.resolve("phase2.log");
    PetriLogger logger = new PetriLogger(logFile.toString());

    try {
      assertTimeoutPreemptively(
          Duration.ofSeconds(5),
          () -> {
            SimulationConfig config =
                new SimulationConfig(
                    new NetConfig(
                        new int[][] {
                          {1, 0},
                          {0, 1}
                        },
                        new int[][] {
                          {1, 0},
                          {0, 0}
                        },
                        new int[] {1, 0},
                        List.of(
                            new TransitionConfig(0, 0L, 999_999L),
                            new TransitionConfig(1, 0L, 999_999L))),
                    List.of(),
                    List.of(
                        new AgentConfig("B", 1, new int[] {1}, -1, null),
                        new AgentConfig("A", 1, new int[] {0}, -1, null)),
                    new LoggingConfig(logFile.toString(), false),
                    new RuntimeConfig(5_000, true),
                    new PolicyConfig("RANDOM", List.of()),
                    new StopConfig(0, 4));

            SimulatorEngine.Components components = SimulatorEngine.build(config, logger);
            List<Thread> threads =
                SimulatorEngine.startAgents(components.agents(), components.monitor());

            for (Thread thread : threads) {
              thread.join(2_000);
            }

            assertTrue(threads.stream().noneMatch(Thread::isAlive));
          });
    } finally {
      logger.close();
    }

    long fireLines =
        Files.readAllLines(logFile).stream()
            .filter(line -> FIRE_LINE.matcher(line).matches())
            .count();
    assertEquals(4, fireLines);
    assertFalse(Files.readAllLines(logFile).isEmpty());
  }
}
