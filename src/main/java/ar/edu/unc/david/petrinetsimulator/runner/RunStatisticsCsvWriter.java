package ar.edu.unc.david.petrinetsimulator.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Writes run statistics and batch aggregations as CSV files. */
public final class RunStatisticsCsvWriter {

  private RunStatisticsCsvWriter() {}

  /** Writes one run summary to a key/value CSV file. */
  public static void writeRunSummary(Path outputPath, RunStatistics stats, int numTransitions) {
    List<String> lines = new ArrayList<>();
    lines.add("metric,value");
    lines.add("runIndex," + stats.runIndex());
    lines.add("networkId," + stats.networkId());
    lines.add("policyType," + stats.policyType());
    lines.add("logFile," + stats.logFile());
    lines.add("durationMs," + stats.durationMs());
    lines.add("totalFires," + stats.totalFires());
    lines.add("transitionInvariantCompositionValid," + stats.transitionInvariantCompositionValid());
    lines.add(
        "transitionInvariantRemainder,"
            + escapeCsv(
                stats.transitionInvariantRemainder() == null
                    ? ""
                    : stats.transitionInvariantRemainder()));

    for (int t = 0; t < numTransitions; t++) {
      int count = stats.transitionCounts().getOrDefault(t, 0);
      lines.add("transition.T" + t + "," + count);
    }

    for (Map.Entry<String, Integer> entry : stats.transitionInvariantCounts().entrySet()) {
      lines.add("invariantCount." + entry.getKey() + "," + entry.getValue());
    }

    for (Map.Entry<String, Double> entry : stats.transitionInvariantPercentages().entrySet()) {
      lines.add(
          "invariantPct."
              + entry.getKey()
              + ","
              + String.format(Locale.ROOT, "%.6f", entry.getValue()));
    }

    writeLines(outputPath, lines);
  }

  /** Writes one row per run with dynamic columns for transition and invariant metrics. */
  public static void writeBatchRuns(
      Path outputPath, List<RunStatistics> runStats, int numTransitions) {
    Set<String> invariantNames = new LinkedHashSet<>();
    for (RunStatistics stats : runStats) {
      invariantNames.addAll(stats.transitionInvariantCounts().keySet());
    }

    List<String> header = new ArrayList<>();
    header.add("runIndex");
    header.add("networkId");
    header.add("policyType");
    header.add("durationMs");
    header.add("totalFires");
    header.add("transitionInvariantCompositionValid");
    header.add("transitionInvariantRemainder");
    for (int t = 0; t < numTransitions; t++) {
      header.add("T" + t);
    }
    for (String name : invariantNames) {
      header.add("count_" + name);
      header.add("pct_" + name);
    }

    List<String> lines = new ArrayList<>();
    lines.add(String.join(",", header));

    for (RunStatistics stats : runStats) {
      List<String> row = new ArrayList<>();
      row.add(Integer.toString(stats.runIndex()));
      row.add(escapeCsv(stats.networkId()));
      row.add(escapeCsv(stats.policyType()));
      row.add(Long.toString(stats.durationMs()));
      row.add(Integer.toString(stats.totalFires()));
      row.add(Boolean.toString(stats.transitionInvariantCompositionValid()));
      row.add(escapeCsv(stats.transitionInvariantRemainder()));
      for (int t = 0; t < numTransitions; t++) {
        row.add(Integer.toString(stats.transitionCounts().getOrDefault(t, 0)));
      }
      for (String name : invariantNames) {
        row.add(Integer.toString(stats.transitionInvariantCounts().getOrDefault(name, 0)));
        row.add(
            String.format(
                Locale.ROOT,
                "%.6f",
                stats.transitionInvariantPercentages().getOrDefault(name, 0.0)));
      }

      lines.add(String.join(",", row));
    }

    writeLines(outputPath, lines);
  }

  /** Writes aggregate statistics (mean/std/min/max) across all runs. */
  public static void writeBatchAggregate(Path outputPath, List<RunStatistics> runStats) {
    Set<String> invariantNames = new LinkedHashSet<>();
    for (RunStatistics stats : runStats) {
      invariantNames.addAll(stats.transitionInvariantPercentages().keySet());
    }

    List<String> lines = new ArrayList<>();
    lines.add("metric,mean,stddev,min,max");
    lines.add(asAggregateRow("durationMs", toDoubles(runStats, RunStatistics::durationMs)));
    lines.add(asAggregateRow("totalFires", toDoubles(runStats, RunStatistics::totalFires)));

    for (String name : invariantNames) {
      lines.add(
          asAggregateRow(
              "pct_" + name,
              runStats.stream()
                  .mapToDouble(s -> s.transitionInvariantPercentages().getOrDefault(name, 0.0))
                  .toArray()));
    }

    writeLines(outputPath, lines);
  }

  private static String asAggregateRow(String metric, double[] values) {
    if (values.length == 0) {
      return metric + ",0.000000,0.000000,0.000000,0.000000";
    }

    DoubleSummaryStatistics stats = java.util.Arrays.stream(values).summaryStatistics();
    double mean = stats.getAverage();
    double variance =
        java.util.Arrays.stream(values)
            .map(
                v -> {
                  double d = v - mean;
                  return d * d;
                })
            .average()
            .orElse(0.0);
    double stddev = Math.sqrt(variance);

    return metric
        + ","
        + String.format(Locale.ROOT, "%.6f", mean)
        + ","
        + String.format(Locale.ROOT, "%.6f", stddev)
        + ","
        + String.format(Locale.ROOT, "%.6f", stats.getMin())
        + ","
        + String.format(Locale.ROOT, "%.6f", stats.getMax());
  }

  private static double[] toDoubles(
      List<RunStatistics> runStats, java.util.function.ToLongFunction<RunStatistics> extractor) {
    return runStats.stream().mapToDouble(s -> extractor.applyAsLong(s)).toArray();
  }

  private static String escapeCsv(String value) {
    String safe = value == null ? "" : value;
    if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
      return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
    return safe;
  }

  private static void writeLines(Path outputPath, List<String> lines) {
    try {
      Path parent = outputPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.write(outputPath, lines);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write CSV: " + outputPath, e);
    }
  }
}
