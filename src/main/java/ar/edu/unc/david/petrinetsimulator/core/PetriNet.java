package ar.edu.unc.david.petrinetsimulator.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Petri net with a given initial marking and a PetriNetMatrix defining the structure
 * of the net.
 */
public class PetriNet {
  private final PetriNetMatrix matrix;
  private final int[] marking;
  private final long[] alpha;
  private final long[] beta;
  private final long[] sensitizationTimestamps;

  /**
   * Constructs a PetriNet with the specified initial marking marking, structure, and timing.
   *
   * @param initialMarking an array representing the initial number of tokens in each place.
   * @param matrix the PetriNetMatrix defining the structure of the Petri net.
   */
  public PetriNet(int[] initialMarking, PetriNetMatrix matrix, long[] alpha, long[] beta) {
    java.util.Objects.requireNonNull(initialMarking, "Initial marking cannot be null");
    java.util.Objects.requireNonNull(matrix, "PetriNetMatrix cannot be null");
    java.util.Objects.requireNonNull(alpha, "Alpha array cannot be null");
    java.util.Objects.requireNonNull(beta, "Beta array cannot be null");

    if (initialMarking.length != matrix.numPlaces()) {
      throw new IllegalArgumentException("Initial marking length mismatch.");
    }
    if (alpha.length != matrix.numTransitions() || beta.length != matrix.numTransitions()) {
      throw new IllegalArgumentException("Timing arrays must match number of transitions.");
    }

    this.matrix = matrix;
    this.marking = initialMarking.clone();
    this.alpha = alpha.clone();
    this.beta = beta.clone();
    this.sensitizationTimestamps = new long[matrix.numTransitions()];

    java.util.Arrays.fill(sensitizationTimestamps, -1);

    updateSensitizationTimestamps();
  }

  /**
   * Checks if the transition with the given index is enabled.
   *
   * @param t the index of the transition to check.
   * @return true if the transition is enabled, false otherwise.
   * @throws IndexOutOfBoundsException if the transition index is out of bounds.
   */
  public boolean isEnabled(int t) {
    int[] pre = matrix.preCol(t);
    for (int p = 0; p < marking.length; p++) {
      if (marking[p] < pre[p]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Fires the transition, updates marking, and refreshes sensitization timestamps.
   *
   * @param t the index of the transition to fire.
   * @throws IndexOutOfBoundsException if the transition index is out of bounds.
   * @throws IllegalStateException if the transition is not enabled and cannot be fired.
   */
  public void fire(int t) {
    if (!isEnabled(t)) {
      throw new IllegalStateException("Transition " + t + " is not enabled");
    }

    int[] a = matrix.incidenceCol(t);
    for (int p = 0; p < marking.length; p++) {
      marking[p] += a[p];
    }

    updateSensitizationTimestamps();
  }

  /**
   * Returns an array of indices of the transitions that are currently enabled.
   *
   * @return an array of indices of the enabled transitions.
   * @throws IndexOutOfBoundsException if any transition index is out of bounds.
   */
  public int[] getEnabledTransitions() {
    List<Integer> enabled = new ArrayList<>();
    for (int t = 0; t < matrix.numTransitions(); t++) {
      if (isEnabled(t)) {
        enabled.add(t);
      }
    }
    return enabled.stream().mapToInt(Integer::intValue).toArray();
  }

  public int[] getMarking() {
    return marking.clone();
  }

  /**
   * Returns the number of tokens in the specified place.
   *
   * @param place the index of the place for which to retrieve the number of tokens.
   * @return the number of tokens in the specified place, which is a non-negative integer.
   * @throws IndexOutOfBoundsException if the place index is out of bounds.
   */
  public int getTokens(int place) {
    if (place < 0 || place >= marking.length) {
      throw new IndexOutOfBoundsException("Place index " + place + " is out of bounds");
    }
    return marking[place];
  }

  /**
   * Checks if the given invariant holds for the current marking.
   *
   * @param invariant an array of integers representing the invariant.
   * @param expectedConstant the expected constant value that the weighted sum.
   * @return true if the invariant holds for the current marking, false otherwise.
   * @throws NullPointerException if the invariant array is null.
   * @throws IllegalArgumentException if the length of the invariant array does not match the number
   *     of places defined in the PetriNetMatrix.
   */
  public boolean checkPlacesInvariant(int[] invariant, int expectedConstant) {
    java.util.Objects.requireNonNull(invariant, "Invariant cannot be null");
    if (invariant.length != marking.length) {
      throw new IllegalArgumentException("Invariant length must match number of places");
    }

    int sum = 0;
    for (int i = 0; i < marking.length; i++) {
      sum += invariant[i] * marking[i];
    }

    return sum == expectedConstant;
  }

  public PetriNetMatrix matrix() {
    return matrix;
  }

  /** Checks if the transition 't' is within its timing window. */
  public boolean isInTimeWindow(int t) {
    long ts = sensitizationTimestamps[t];
    if (ts == -1) {
      return false;
    }
    long elapsed = System.currentTimeMillis() - ts;
    return elapsed >= alpha[t] && elapsed <= beta[t];
  }

  /** Updates the sensitization timestamps for all transitions based on current marking. */
  public void updateSensitizationTimestamps() {
    long now = System.currentTimeMillis();
    for (int t = 0; t < matrix.numTransitions(); t++) {
      if (isEnabled(t)) {
        if (sensitizationTimestamps[t] == -1) {
          sensitizationTimestamps[t] = now;
        }
      } else {
        sensitizationTimestamps[t] = -1;
      }
    }
  }

  /**
   * Returns the remaining time in nanoseconds until transition 't' can be fired, or Long.MAX_VALUE
   * if it is not sensitized.
   */
  public long nanosUntilOpen(int t) {
    long ts = sensitizationTimestamps[t];
    if (ts == -1) {
      return Long.MAX_VALUE;
    }
    long elapsed = System.currentTimeMillis() - ts;
    long remaining = alpha[t] - elapsed;
    return remaining <= 0 ? 0 : remaining * 1_000_000L;
  }

  /** Checks if the transition 't' has exceeded its timing window. */
  public boolean isExpired(int t) {
    long ts = sensitizationTimestamps[t];
    if (ts == -1) {
      return false;
    }
    long elapsed = System.currentTimeMillis() - ts;
    return elapsed > beta[t];
  }
}
