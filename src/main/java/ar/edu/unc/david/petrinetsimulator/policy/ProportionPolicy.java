package ar.edu.unc.david.petrinetsimulator.policy;

import ar.edu.unc.david.petrinetsimulator.config.logic.PolicyGroupConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Policy that selects candidates according to weighted proportions.
 *
 * <p>The policy tracks how many times each transition was selected and chooses, among the current
 * candidates, the one whose observed selection ratio is furthest below its configured weight.
 */
public class ProportionPolicy implements Policy {
  private final List<PolicyGroupConfig> groups;
  private final Map<Integer, Long> selectionCounts = new HashMap<>();

  public ProportionPolicy(List<PolicyGroupConfig> groups) {
    Objects.requireNonNull(groups, "groups cannot be null");
    if (groups.isEmpty()) {
      throw new IllegalArgumentException("ProportionPolicy requires at least one group");
    }
    this.groups = groups;
  }

  @Override
  public synchronized int decide(int[] candidates) {
    Objects.requireNonNull(candidates, "candidates array cannot be null");
    if (candidates.length == 0) {
      throw new IllegalArgumentException("No candidates to choose from");
    }

    int chosen = chooseBestCandidate(candidates);
    selectionCounts.merge(chosen, 1L, Long::sum);
    return chosen;
  }

  private int chooseBestCandidate(int[] candidates) {
    int bestCandidate = candidates[0];
    double bestScore = Double.POSITIVE_INFINITY;
    int bestOrder = Integer.MAX_VALUE;

    for (int index = 0; index < candidates.length; index++) {
      int candidate = candidates[index];
      PolicyGroupConfig group = findGroup(candidate);
      double score = score(candidate, group);

      if (score < bestScore || (score == bestScore && index < bestOrder)) {
        bestCandidate = candidate;
        bestScore = score;
        bestOrder = index;
      }
    }

    return bestCandidate;
  }

  private double score(int candidate, PolicyGroupConfig group) {
    long observed = selectionCounts.getOrDefault(candidate, 0L);
    if (group == null) {
      return observed;
    }

    int transitionIndex = indexOf(group.transitions(), candidate);
    int weight = group.weights()[transitionIndex];
    return observed / (double) weight;
  }

  private PolicyGroupConfig findGroup(int candidate) {
    for (PolicyGroupConfig group : groups) {
      if (indexOf(group.transitions(), candidate) >= 0) {
        return group;
      }
    }
    return null;
  }

  private int indexOf(int[] values, int candidate) {
    for (int i = 0; i < values.length; i++) {
      if (values[i] == candidate) {
        return i;
      }
    }
    return -1;
  }
}
