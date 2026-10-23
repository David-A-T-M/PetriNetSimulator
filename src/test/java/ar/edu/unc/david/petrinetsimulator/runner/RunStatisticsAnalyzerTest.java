package ar.edu.unc.david.petrinetsimulator.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.unc.david.petrinetsimulator.config.logic.AgentConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.LoggingConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.NetConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.PlaceInvariantConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.PolicyConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.RuntimeConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.SimulationConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.StopConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.TransitionConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.TransitionInvariantConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for phase 7 run statistics analyzer. */
class RunStatisticsAnalyzerTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("analyzer computes transition and invariant stats from one valid log")
  void analyzerComputesTransitionAndInvariantStats() throws IOException {
    Path logPath = tempDir.resolve("run.csv");
    Files.write(
        logPath,
        List.of(
            "Timestamp,Thread,Transition,MarkingBefore,MarkingAfter",
            "1700000001000,T-0,0,\"[1; 0]\",\"[1; 0]\"",
            "1700000001010,T-0,1,\"[1; 0]\",\"[1; 0]\"",
            "1700000001020,T-0,5,\"[1; 0]\",\"[1; 0]\"",
            "1700000001030,T-0,6,\"[1; 0]\",\"[1; 0]\"",
            "1700000001040,T-0,11,\"[1; 0]\",\"[1; 0]\"",
            "--- End of simulation: 2026-03-24T10:00:00 ---"));

    SimulationConfig config =
        new SimulationConfig(
            new NetConfig(
                new int[][] {{1}},
                new int[][] {{1}},
                new int[] {1},
                List.of(new TransitionConfig(0, 0L, 9_999L))),
            List.of(new PlaceInvariantConfig(new int[] {1}, 1)),
            List.of(new AgentConfig("A", 1, new int[] {0}, 1, null)),
            new LoggingConfig(logPath.toString(), false),
            new RuntimeConfig(1_000, true),
            new PolicyConfig("RANDOM", List.of()),
            new StopConfig(11, 1),
            List.of(new TransitionInvariantConfig("simple", new int[] {0, 1, 5, 6, 11})));

    RunStatistics stats = RunStatisticsAnalyzer.analyze(1, "data_proc", "random", logPath, config);

    assertEquals(1, stats.runIndex());
    assertEquals(40L, stats.durationMs());
    assertEquals(5, stats.totalFires());
    assertEquals(1, stats.transitionCounts().getOrDefault(0, 0));
    assertEquals(1, stats.transitionCounts().getOrDefault(11, 0));
    assertEquals(1, stats.transitionInvariantCounts().getOrDefault("simple", 0));
    assertTrue(stats.transitionInvariantCompositionValid());
    assertTrue(stats.transitionInvariantRemainder().isBlank());
    assertEquals(100.0, stats.transitionInvariantPercentages().getOrDefault("simple", 0.0), 0.0001);
  }

  @Test
  @DisplayName("analyzer reports invalid composition when sequence is incomplete")
  void analyzerReportsInvalidComposition() throws IOException {
    Path logPath = tempDir.resolve("run_invalid.csv");
    Files.write(
        logPath,
        List.of(
            "Timestamp,Thread,Transition,MarkingBefore,MarkingAfter",
            "1700000001000,T-0,0,\"[1; 0]\",\"[1; 0]\"",
            "1700000001010,T-0,1,\"[1; 0]\",\"[1; 0]\"",
            "--- End of simulation: 2026-03-24T10:00:00 ---"));

    SimulationConfig config =
        new SimulationConfig(
            new NetConfig(
                new int[][] {{1}},
                new int[][] {{1}},
                new int[] {1},
                List.of(new TransitionConfig(0, 0L, 9_999L))),
            List.of(new PlaceInvariantConfig(new int[] {1}, 1)),
            List.of(new AgentConfig("A", 1, new int[] {0}, 1, null)),
            new LoggingConfig(logPath.toString(), false),
            new RuntimeConfig(1_000, true),
            new PolicyConfig("RANDOM", List.of()),
            new StopConfig(11, 1),
            List.of(new TransitionInvariantConfig("simple", new int[] {0, 1, 5, 6, 11})));

    RunStatistics stats = RunStatisticsAnalyzer.analyze(1, "data_proc", "random", logPath, config);

    assertFalse(stats.transitionInvariantCompositionValid());
    assertFalse(stats.transitionInvariantRemainder().isBlank());
  }
}
