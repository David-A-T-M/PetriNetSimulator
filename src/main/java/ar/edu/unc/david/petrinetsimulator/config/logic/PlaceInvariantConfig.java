package ar.edu.unc.david.petrinetsimulator.config.logic;

/** Configuration for a place invariant in the Petri net. */
public record PlaceInvariantConfig(int[] places, int constant) {
  /** Validates the place invariant configuration. */
  public PlaceInvariantConfig {
    if (places == null || places.length == 0) {
      throw new IllegalArgumentException("Invariants must specify at least one place.");
    }

    for (int p : places) {
      if (p < 0) {
        throw new IllegalArgumentException("Place indices in invariants cannot be negative.");
      }
    }

    if (constant < 0) {
      throw new IllegalArgumentException("Invariant constant cannot be negative.");
    }
  }
}
