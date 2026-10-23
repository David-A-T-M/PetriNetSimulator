package ar.edu.unc.david.petrinetsimulator.runner;

import ar.edu.unc.david.petrinetsimulator.config.logic.SimulationConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.TransitionInvariantConfig;
import ar.edu.unc.david.petrinetsimulator.log.PetriLogPatterns;
import ar.edu.unc.david.petrinetsimulator.log.PetriLogValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Computes run-level statistics by reading one simulation log file. */
public final class RunStatisticsAnalyzer {
  private RunStatisticsAnalyzer() {}

  /** Builds a statistics summary for one run. */
  public static RunStatistics analyze(
      int runIndex, String networkId, String policyType, Path logPath, SimulationConfig config) {
    List<String> lines;
    try {
      lines = Files.readAllLines(logPath);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read log file for run summary: " + logPath, e);
    }

    List<Integer> transitions = PetriLogValidator.fireTransitionsFromLines(lines);
    List<Long> timestamps = fireTimestamps(lines);

    Map<Integer, Integer> transitionCounts = new TreeMap<>();
    for (int transition : transitions) {
      transitionCounts.put(transition, transitionCounts.getOrDefault(transition, 0) + 1);
    }

    long durationMs = timestamps.size() < 2 ? 0 : timestamps.getLast() - timestamps.getFirst();

    List<TransitionInvariantConfig> invariants =
        config.transitionInvariants() == null ? List.of() : config.transitionInvariants();
    PetriLogValidator.CompositionAnalysis composition =
        PetriLogValidator.analyzeComposition(transitions, invariants);

    Map<String, Integer> invariantCounts = new LinkedHashMap<>();
    for (TransitionInvariantConfig invariant : invariants) {
      invariantCounts.put(
          invariant.name(), composition.invariantCounts().getOrDefault(invariant.name(), 0));
    }

    int totalInvariantCompletions =
        invariantCounts.values().stream().mapToInt(Integer::intValue).sum();

    Map<String, Double> invariantPercentages = new LinkedHashMap<>();
    for (Map.Entry<String, Integer> entry : invariantCounts.entrySet()) {
      double percentage =
          totalInvariantCompletions == 0
              ? 0.0
              : (entry.getValue() * 100.0) / totalInvariantCompletions;
      invariantPercentages.put(entry.getKey(), percentage);
    }

    return new RunStatistics(
        runIndex,
        networkId,
        policyType,
        logPath.toString(),
        durationMs,
        transitions.size(),
        Map.copyOf(transitionCounts),
        Map.copyOf(invariantCounts),
        Map.copyOf(invariantPercentages),
        composition.isValid(),
        composition.remainingSequence());
  }

  private static List<Long> fireTimestamps(List<String> lines) {
    List<Long> timestamps = new ArrayList<>();
    if (lines == null || lines.size() < 2) {
      return timestamps;
    }

    for (int i = 1; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line != null && PetriLogPatterns.FIRE_EVENT.matcher(line).matches()) {
        int firstComma = line.indexOf(',');
        if (firstComma > 0) {
          timestamps.add(Long.parseLong(line, 0, firstComma, 10));
        }
      }
    }
    return timestamps;
  }
}
