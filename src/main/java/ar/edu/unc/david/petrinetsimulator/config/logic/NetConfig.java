package ar.edu.unc.david.petrinetsimulator.config.logic;

import java.util.List;

/** Configuration for the Petri net structure. */
public record NetConfig(
    int[][] pre, int[][] post, int[] initialMarking, List<TransitionConfig> transitions) {
  /** Validates the NetConfig parameters. */
  public NetConfig {
    if (pre.length == 0 || post.length == 0) {
      throw new IllegalArgumentException("Matrices cannot be empty.");
    }
    if (pre.length != post.length || pre.length != initialMarking.length) {
      throw new IllegalArgumentException(
          "Number of places must match in pre, post, and initial marking.");
    }
    int numTransitions = pre[0].length;
    if (numTransitions != post[0].length) {
      throw new IllegalArgumentException(
          "Number of transitions must match in pre and post matrices.");
    }
    if (transitions == null || transitions.size() != numTransitions) {
      throw new IllegalArgumentException(
          "Must provide timing configuration for exactly " + numTransitions + " transitions.");
    }

    boolean[] idCheck = new boolean[numTransitions];
    for (TransitionConfig t : transitions) {
      if (t.id() >= numTransitions) {
        throw new IllegalArgumentException("Transition ID " + t.id() + " is out of bounds.");
      }
      idCheck[t.id()] = true;
    }

    for (int i = 0; i < idCheck.length; i++) {
      if (!idCheck[i]) {
        throw new IllegalArgumentException("Missing timing config for transition " + i);
      }
    }
  }
}
