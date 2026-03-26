package ar.edu.unc.david.petrinetsimulator.config;

/** Configuration for the policy used in the Petri net simulation. */
public record PolicyConfig(String type) {
  /** Validates the policy configuration. */
  public PolicyConfig {
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("Policy type must be specified and cannot be blank.");
    }

    String t = type.toUpperCase();
    if (!t.equals("RANDOM")) {
      throw new IllegalArgumentException("Policy type '" + type + "' is not supported.");
    }
  }
}
