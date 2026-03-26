package ar.edu.unc.david.petrinetsimulator.policy;

/** Factory class for creating Policy instances based on configuration. */
public class PolicyFactory {
  /** Creates a Policy instance based on the provided configuration. */
  public static Policy create(String type) {

    return switch (type) {
      case "RANDOM" -> new RandomPolicy();
      default -> {
        throw new IllegalArgumentException("Unsupported policy type: " + type);
      }
    };
  }
}
