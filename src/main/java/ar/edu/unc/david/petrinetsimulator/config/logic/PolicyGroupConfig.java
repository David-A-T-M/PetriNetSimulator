package ar.edu.unc.david.petrinetsimulator.config.logic;

import java.util.Objects;

/** Configuration for a group of transitions controlled by a policy. */
public record PolicyGroupConfig(int[] transitions, int[] weights) {
  /** Validates the policy group configuration. */
  public PolicyGroupConfig {
    Objects.requireNonNull(transitions, "Transitions cannot be null.");
    Objects.requireNonNull(weights, "Weights cannot be null.");
    if (transitions.length == 0) {
      throw new IllegalArgumentException("Policy group must contain at least one transition.");
    }
    if (weights.length == 0) {
      throw new IllegalArgumentException("Policy group must contain at least one weight.");
    }
    if (transitions.length != weights.length) {
      throw new IllegalArgumentException("Transitions and weights must have the same length.");
    }

    for (int transition : transitions) {
      if (transition < 0) {
        throw new IllegalArgumentException("Transition indices must be non-negative.");
      }
    }
    for (int weight : weights) {
      if (weight <= 0) {
        throw new IllegalArgumentException("Weights must be positive.");
      }
    }

    transitions = transitions.clone();
    weights = weights.clone();
  }
}
