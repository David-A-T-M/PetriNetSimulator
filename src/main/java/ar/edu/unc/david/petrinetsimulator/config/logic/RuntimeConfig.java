package ar.edu.unc.david.petrinetsimulator.config.logic;

/** Runtime configuration for the Petri net simulation. */
public record RuntimeConfig(int joinTimeoutMs, boolean builtInValidation) {
  /** Validates the runtime configuration. */
  public RuntimeConfig {
    if (joinTimeoutMs < 0) {
      throw new IllegalArgumentException("Timeout must be non-negative");
    }
  }
}
