package ar.edu.unc.david.petrinetsimulator.core;

/** Factory class for creating Petri net instances for testing purposes. */
public class PetriTestFactory {
  /**
   * Factory method to create a Petri net representing the producer-consumer problem.
   *
   * @return a PetriNet instance representing the producer-consumer problem with a specific initial
   *     marking.
   */
  public static PetriNet createProducerConsumer() {
    PetriNetMatrix matrix = fromProducerConsumer();

    // P0=2 (Producers), P3=2 (Consumers), P6=3 (Buffer size), P7=1 (Mutex)
    int[] m0 = {2, 0, 0, 2, 0, 0, 3, 1, 0};

    return new PetriNet(m0, matrix);
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
}
