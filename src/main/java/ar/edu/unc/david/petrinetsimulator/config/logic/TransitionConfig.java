package ar.edu.unc.david.petrinetsimulator.config.logic;

/** Configuration for a single transition's timing. */
public record TransitionConfig(int id, long alpha, long beta) {
  /** Validates the TransitionConfig parameters. */
  public TransitionConfig {
    if (id < 0) {
      throw new IllegalArgumentException("Transition ID cannot be negative.");
    }
    if (alpha < 0 || beta < 0) {
      throw new IllegalArgumentException("Time intervals cannot be negative.");
    }
    if (alpha > beta) {
      throw new IllegalArgumentException("Alpha cannot be greater than Beta.");
    }
  }
}
