package ar.edu.unc.david.petrinetsimulator.config.logic;

/** Configuration for a transition invariant sequence in the Petri net. */
public record TransitionInvariantConfig(String name, int[] sequence) {
  /** Validates transition invariant configuration. */
  public TransitionInvariantConfig {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Transition invariant name cannot be null or blank.");
    }

    if (sequence == null || sequence.length == 0) {
      throw new IllegalArgumentException(
          "Transition invariant sequence must contain at least one transition.");
    }

    for (int transition : sequence) {
      if (transition < 0) {
        throw new IllegalArgumentException(
            "Transition invariant sequence cannot contain negative transition IDs.");
      }
    }
  }
}
