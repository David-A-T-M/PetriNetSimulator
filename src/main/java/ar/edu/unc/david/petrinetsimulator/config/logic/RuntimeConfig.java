package ar.edu.unc.david.petrinetsimulator.config.logic;

/** Runtime configuration for the Petri net simulation. */
public record RuntimeConfig(
    int joinTimeoutMs, boolean builtInValidation, Boolean checkPlaceInvariants) {
  /** Validates the runtime configuration. */
  public RuntimeConfig {
    if (joinTimeoutMs < 0) {
      throw new IllegalArgumentException("Timeout must be non-negative");
    }

    checkPlaceInvariants = checkPlaceInvariants == null ? Boolean.TRUE : checkPlaceInvariants;
  }

  /** Backward-compatible constructor with place invariant checks enabled by default. */
  public RuntimeConfig(int joinTimeoutMs, boolean builtInValidation) {
    this(joinTimeoutMs, builtInValidation, true);
  }
}
