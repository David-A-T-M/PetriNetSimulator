package ar.edu.unc.david.petrinetsimulator.core;

import ar.edu.unc.david.petrinetsimulator.config.TransitionConfig;
import java.util.Arrays;
import java.util.List;

/** Factory class for creating Petri net instances for testing purposes. */
public class PetriTestFactory {
  /**
   * Factory method to create a Petri net representing the producer-consumer problem.
   *
   * @return a PetriNet instance representing the producer-consumer problem with a specific initial
   *     marking and default timing.
   */
  public static PetriNet createProducerConsumer() {
    PetriNetMatrix matrix = fromProducerConsumer();

    // P0=2 (Producers), P3=2 (Consumers), P6=3 (Buffer size), P7=1 (Mutex)
    int[] m0 = {2, 0, 0, 2, 0, 0, 3, 1, 0};

    // Default timing: alpha=100ms, beta=100ms for all transitions
    long[] alpha = {100, 100, 100, 100, 100, 100};
    long[] beta = {100, 100, 100, 100, 100, 100};

    return new PetriNet(m0, matrix, alpha, beta);
  }

  /**
   * Factory method to create a Petri net with custom timing.
   *
   * @param alpha array of alpha (min delay) for each transition
   * @param beta array of beta (max delay) for each transition
   * @return a PetriNet instance with the specified timing
   * @throws IllegalArgumentException if alpha or beta arrays have incorrect length
   */
  public static PetriNet createProducerConsumerWithTiming(long[] alpha, long[] beta) {
    PetriNetMatrix matrix = fromProducerConsumer();
    int[] m0 = {2, 0, 0, 2, 0, 0, 3, 1, 0};

    if (alpha.length != 6 || beta.length != 6) {
      throw new IllegalArgumentException(
          "Timing arrays must have length 6 (number of transitions)");
    }

    return new PetriNet(m0, matrix, alpha, beta);
  }

  /**
   * Creates a PetriNetMatrix for a producer-consumer example.
   *
   * @return a PetriNetMatrix instance representing a producer-consumer example
   */
  public static PetriNetMatrix fromProducerConsumer() {
    int[][] pre = {
      {1, 0, 0, 0, 0, 0},
      {0, 1, 0, 0, 0, 0},
      {0, 0, 1, 0, 0, 0},
      {0, 0, 0, 1, 0, 0},
      {0, 0, 0, 0, 1, 0},
      {0, 0, 0, 0, 0, 1},
      {1, 0, 0, 0, 0, 0},
      {1, 0, 0, 1, 0, 0},
      {0, 0, 0, 1, 0, 0}
    };
    int[][] post = {
      {0, 0, 1, 0, 0, 0},
      {1, 0, 0, 0, 0, 0},
      {0, 1, 0, 0, 0, 0},
      {0, 0, 0, 0, 0, 1},
      {0, 0, 0, 1, 0, 0},
      {0, 0, 0, 0, 1, 0},
      {0, 0, 0, 0, 1, 0},
      {0, 1, 0, 0, 1, 0},
      {0, 1, 0, 0, 0, 0}
    };
    return new PetriNetMatrix(pre, post);
  }

  /**
   * Creates a list of TransitionConfig objects for the producer-consumer network.
   *
   * @param defaultAlpha default alpha value for all transitions
   * @param defaultBeta default beta value for all transitions
   * @return a List of TransitionConfig with the specified timing
   */
  public static List<TransitionConfig> createProducerConsumerTransitionConfigs(
      long defaultAlpha, long defaultBeta) {
    return Arrays.asList(
        new TransitionConfig(0, defaultAlpha, defaultBeta),
        new TransitionConfig(1, defaultAlpha, defaultBeta),
        new TransitionConfig(2, defaultAlpha, defaultBeta),
        new TransitionConfig(3, defaultAlpha, defaultBeta),
        new TransitionConfig(4, defaultAlpha, defaultBeta),
        new TransitionConfig(5, defaultAlpha, defaultBeta));
  }
}
