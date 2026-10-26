package ar.edu.unc.david.petrinetsimulator.config.logic;

/** Configuration for the global stop condition of a simulation run. */
public record StopConfig(int transition, int count, Integer admission) {
  /** Validates the stop configuration. */
  public StopConfig {
    if (transition < 0) {
      throw new IllegalArgumentException("Stop transition must be non-negative.");
    }
    if (count <= 0) {
      throw new IllegalArgumentException("Stop count must be positive.");
    }
    if (admission != null && admission < 0) {
      throw new IllegalArgumentException("Admission transition must be non-negative.");
    }
    if (admission != null && admission == transition) {
      throw new IllegalArgumentException("Admission transition must differ from stop transition.");
    }
  }

  public StopConfig(int transition, int count) {
    this(transition, count, null);
  }
}
