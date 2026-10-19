package ar.edu.unc.david.petrinetsimulator.policy;

import ar.edu.unc.david.petrinetsimulator.config.logic.PolicyConfig;
import java.util.Locale;
import java.util.Objects;

/** Factory class for creating Policy instances based on configuration. */
public class PolicyFactory {
  /** Creates a Policy instance based on the provided configuration type. */
  public static Policy create(String type) {

    return switch (type.toUpperCase(Locale.ROOT)) {
      case "RANDOM" -> new RandomPolicy();
      default -> throw new IllegalArgumentException("Unsupported policy type: " + type);
    };
  }

  /** Creates a Policy instance based on the provided policy configuration. */
  public static Policy create(PolicyConfig config) {
    Objects.requireNonNull(config, "config cannot be null");

    return switch (config.type().toUpperCase(Locale.ROOT)) {
      case "RANDOM" -> new RandomPolicy();
      case "PROPORTION" -> new ProportionPolicy(config.groups());
      default -> throw new IllegalArgumentException("Unsupported policy type: " + config.type());
    };
  }
}
