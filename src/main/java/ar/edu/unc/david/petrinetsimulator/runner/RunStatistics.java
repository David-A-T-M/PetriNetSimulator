package ar.edu.unc.david.petrinetsimulator.runner;

import java.util.Map;

/** Summary metrics for one simulation run. */
public record RunStatistics(
    int runIndex,
    String networkId,
    String policyType,
    String logFile,
    long durationMs,
    int totalFires,
    Map<Integer, Integer> transitionCounts,
    Map<String, Integer> transitionInvariantCounts,
    Map<String, Double> transitionInvariantPercentages,
    boolean transitionInvariantCompositionValid,
    String transitionInvariantRemainder) {}
