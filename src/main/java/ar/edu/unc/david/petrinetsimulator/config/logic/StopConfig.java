package ar.edu.unc.david.petrinetsimulator.config.logic;

/** Configuration for the global stop condition of a simulation run. */
public record StopConfig(int transition, int count) {
  /** Validates the stop configuration. */
  public StopConfig {
    if (transition < 0) {
      throw new IllegalArgumentException("Stop transition must be non-negative.");
    }
    if (count <= 0) {
      throw new IllegalArgumentException("Stop count must be positive.");
    }
  }
}
