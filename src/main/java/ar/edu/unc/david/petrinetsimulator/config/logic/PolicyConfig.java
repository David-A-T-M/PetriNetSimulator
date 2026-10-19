package ar.edu.unc.david.petrinetsimulator.config.logic;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Configuration for the policy used in the Petri net simulation. */
public record PolicyConfig(String type, List<PolicyGroupConfig> groups) {
  /** Validates the policy configuration. */
  public PolicyConfig {
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("Policy type must be specified and cannot be blank.");
    }

    String normalizedType = type.toUpperCase(Locale.ROOT);
    if (!normalizedType.equals("RANDOM") && !normalizedType.equals("PROPORTION")) {
      throw new IllegalArgumentException("Policy type '" + type + "' is not supported.");
    }

    groups = groups == null ? List.of() : List.copyOf(groups);
    if (normalizedType.equals("PROPORTION") && groups.isEmpty()) {
      throw new IllegalArgumentException("PROPORTION policy requires at least one group.");
    }

    Objects.requireNonNull(groups, "Policy groups cannot be null.");
  }
}
