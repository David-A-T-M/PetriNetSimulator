package ar.edu.unc.david.petrinetsimulator.monitor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Integration tests for admission-capped stop behavior. */
class MonitorAdmissionTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("admission cap closes input transition and run ends with exact completed count")
  void admissionCapClosesInputTransitionAndRunEndsCleanly() throws Exception {
    Path logFile = tempDir.resolve("admission.log");
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
                          {0, 1},
                          {1, 0}
                        },
                        new int[] {3, 0},
                        List.of(
                            new TransitionConfig(0, 0L, 999_999L),
                            new TransitionConfig(1, 0L, 999_999L))),
                    List.of(),
                    List.of(
                        new AgentConfig("A", 2, new int[] {0}, -1, null),
                        new AgentConfig("B", 2, new int[] {1}, -1, null)),
                    new LoggingConfig(logFile.toString(), false),
                    new RuntimeConfig(5_000, true),
                    new PolicyConfig("RANDOM", List.of()),
                    new StopConfig(1, 5, 0),
                    List.of());

            SimulatorEngine.Components components = SimulatorEngine.build(config, logger);
            List<Thread> threads =
                SimulatorEngine.startAgents(components.agents(), components.monitor());

            for (Thread thread : threads) {
              thread.join(2_000);
            }

            assertTrue(threads.stream().noneMatch(Thread::isAlive));
            assertArrayEquals(new int[] {3, 0}, components.net().getMarking());
            assertTrue(components.stopCondition().isReached());
          });
    } finally {
      logger.close();
    }

    int t0Count = countTransition(logFile, 0);
    int t1Count = countTransition(logFile, 1);

    assertEquals(5, t0Count);
    assertEquals(5, t1Count);
  }

  @Test
  @DisplayName("stop condition rejects admission transition after reaching admission target")
  void stopConditionRejectsAdmissionTransitionAfterTarget() {
    StopCondition stop = new StopCondition(1, 2, 0);

    assertFalse(stop.rejects(0));
    assertFalse(stop.recordFire(0));
    assertFalse(stop.rejects(0));

    assertFalse(stop.recordFire(0));
    assertTrue(stop.rejects(0));
    assertFalse(stop.isReached());

    assertFalse(stop.recordFire(0));
    assertFalse(stop.recordFire(1));
    assertFalse(stop.isReached());
    assertTrue(stop.recordFire(1));
    assertTrue(stop.isReached());
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
