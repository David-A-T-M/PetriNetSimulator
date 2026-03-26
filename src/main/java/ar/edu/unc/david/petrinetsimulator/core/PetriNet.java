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

  /**
   * Constructs a PetriNet with the specified initial marking and PetriNetMatrix.
   *
   * @param initialMarking an array representing the initial number of tokens in each place.
   * @param matrix the PetriNetMatrix defining the structure of the Petri net.
   * @throws NullPointerException if initialMarking or matrix is null.
   * @throws IllegalArgumentException if the length of initialMarking does not match the number of
   *     places defined in the matrix.
   */
  public PetriNet(int[] initialMarking, PetriNetMatrix matrix) {
    java.util.Objects.requireNonNull(initialMarking, "Initial marking cannot be null");
    java.util.Objects.requireNonNull(matrix, "PetriNetMatrix cannot be null");

    if (initialMarking.length != matrix.numPlaces()) {
      throw new IllegalArgumentException(
          "Initial marking length ("
              + initialMarking.length
              + ") must match number of places ("
              + matrix.numPlaces()
              + ")");
    }

    this.matrix = matrix;
    this.marking = initialMarking.clone();
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
   * Fires the transition with the given index, updating the marking according to the incidence
   * matrix of the PetriNetMatrix.
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
}
