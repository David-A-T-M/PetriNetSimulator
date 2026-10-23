package ar.edu.unc.david.petrinetsimulator.runner;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for phase 7 CSV exports. */
class RunStatisticsCsvWriterTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("writer exports run, per-run batch, and aggregate CSV files")
  void writerExportsAllCsvFiles() throws IOException {
    RunStatistics run1 =
        new RunStatistics(
            1,
            "data_proc",
            "random",
            "runs/data_proc_random_run001.csv",
            100,
            10,
            Map.of(0, 3, 1, 7),
            Map.of("simple", 2, "high", 1),
            Map.of("simple", 66.666667, "high", 33.333333),
            true,
            "");

    RunStatistics run2 =
        new RunStatistics(
            2,
            "data_proc",
            "random",
            "runs/data_proc_random_run002.csv",
            140,
            14,
            Map.of(0, 5, 1, 9),
            Map.of("simple", 3, "high", 1),
            Map.of("simple", 75.0, "high", 25.0),
            true,
            "");

    Path runSummary = tempDir.resolve("run001_summary.csv");
    Path runsCsv = tempDir.resolve("runs.csv");
    Path aggregateCsv = tempDir.resolve("batch_summary.csv");

    RunStatisticsCsvWriter.writeRunSummary(runSummary, run1, 4);
    RunStatisticsCsvWriter.writeBatchRuns(runsCsv, List.of(run1, run2), 4);
    RunStatisticsCsvWriter.writeBatchAggregate(aggregateCsv, List.of(run1, run2));

    String runSummaryContent = Files.readString(runSummary);
    assertTrue(runSummaryContent.contains("metric,value"));
    assertTrue(runSummaryContent.contains("transition.T0,3"));
    assertTrue(runSummaryContent.contains("invariantPct.simple,66.666667"));

    String runsContent = Files.readString(runsCsv);
    assertTrue(runsContent.contains("runIndex,networkId,policyType"));
    assertTrue(runsContent.contains("count_simple"));
    assertTrue(runsContent.contains("pct_high"));

    String aggregateContent = Files.readString(aggregateCsv);
    assertTrue(aggregateContent.contains("metric,mean,stddev,min,max"));
    assertTrue(aggregateContent.contains("durationMs,120.000000"));
    assertTrue(aggregateContent.contains("totalFires,12.000000"));
  }
}
