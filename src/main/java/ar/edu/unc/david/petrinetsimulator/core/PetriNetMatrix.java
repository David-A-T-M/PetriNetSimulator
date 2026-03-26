package ar.edu.unc.david.petrinetsimulator.core;

/**
 * Represents a Petri net using pre and incidence matrices. The pre-matrix (W-) is stored in a
 * transition-oriented format (transitions as rows, places as columns) for efficient access during
 * firing. The incidence matrix (C) is also stored in the same format.
 */
public class PetriNetMatrix {
  private final int[][] preT; // W-
  private final int[][] incidenceT; // C
  private final int numPlaces;
  private final int numTransitions;

  /**
   * Constructs a PetriNetMatrix from given pre- and post-matrices. The pre-matrix is expected in
   * [p][t] format. Input validation ensures that the matrices are non-null, have consistent
   * dimensions, and are not jagged.
   *
   * @param pre the pre-matrix (W-) in [p][t] format.
   * @param post the post-matrix (W+) in [p][t] format.
   * @throws NullPointerException if either pre or post is null.
   * @throws IllegalArgumentException if the matrices have inconsistent dimensions, are empty, or
   *     contain null rows.
   */
  public PetriNetMatrix(int[][] pre, int[][] post) {
    validateInput(pre, post);

    this.numPlaces = pre.length;
    this.numTransitions = pre[0].length;

    this.preT = new int[numTransitions][numPlaces];
    this.incidenceT = new int[numTransitions][numPlaces];

    for (int t = 0; t < numTransitions; t++) {
      for (int p = 0; p < numPlaces; p++) {
        this.preT[t][p] = pre[p][t];
        this.incidenceT[t][p] = post[p][t] - pre[p][t];
      }
    }
  }

  private void validateInput(int[][] pre, int[][] post) {
    java.util.Objects.requireNonNull(pre, "Pre matrix cannot be null");
    java.util.Objects.requireNonNull(post, "Post matrix cannot be null");

    if (pre.length == 0 || post.length == 0) {
      throw new IllegalArgumentException("Matrices cannot be empty");
    }

    int expectedTransitions = pre[0].length;
    if (expectedTransitions == 0) {
      throw new IllegalArgumentException("Matrices must have at least one transition");
    }

    if (pre.length != post.length) {
      throw new IllegalArgumentException("Pre and post must have the same number of places");
    }

    for (int i = 0; i < pre.length; i++) {
      if (pre[i] == null || post[i] == null) {
        throw new IllegalArgumentException("Matrix rows cannot be null");
      }
      if (pre[i].length != expectedTransitions || post[i].length != expectedTransitions) {
        throw new IllegalArgumentException(
            "All rows must have the same number of transitions (non-jagged)");
      }
    }
  }

  public int[] preCol(int t) {
    return preT[t];
  }

  public int[] incidenceCol(int t) {
    return incidenceT[t];
  }

  public int numPlaces() {
    return numPlaces;
  }

  public int numTransitions() {
    return numTransitions;
  }
}
