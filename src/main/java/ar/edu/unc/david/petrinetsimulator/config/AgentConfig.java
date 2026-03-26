package ar.edu.unc.david.petrinetsimulator.config;

/** Configuration for an agent in the Petri net simulation. */
public record AgentConfig(String namePrefix, int count, int[] sequence, int cycles) {
  /** Validates the agent configuration. */
  public AgentConfig {
    if (count <= 0) {
      throw new IllegalArgumentException("The number of agents must be positive.");
    }
    if (cycles <= 0) {
      throw new IllegalArgumentException("The number of cycles must be positive.");
    }
    if (namePrefix == null || namePrefix.isBlank()) {
      throw new IllegalArgumentException(
          "The name prefix for agents must be specified and cannot be blank.");
    }
    if (sequence == null || sequence.length == 0) {
      throw new IllegalArgumentException(
          "The sequence of transitions for agents must be specified and cannot be empty.");
    }
  }
}
