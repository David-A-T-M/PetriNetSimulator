package ar.edu.unc.david.petrinetsimulator.config;

/** Configuration for the Petri net structure. */
public record NetConfig(int[][] pre, int[][] post, int[] initialMarking) {
  /** Validates the NetConfig parameters. */
  public NetConfig {
    if (pre.length == 0 || post.length == 0) {
      throw new IllegalArgumentException("Matrices cannot be empty.");
    }
    if (pre.length != post.length || pre.length != initialMarking.length) {
      throw new IllegalArgumentException(
          "Number of places must match in pre, post, and initial marking.");
    }
    if (pre[0].length != post[0].length) {
      throw new IllegalArgumentException(
          "Number of transitions must match in pre and post matrices.");
    }
  }
}
