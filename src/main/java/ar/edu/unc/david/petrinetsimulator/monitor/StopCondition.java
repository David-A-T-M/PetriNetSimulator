package ar.edu.unc.david.petrinetsimulator.monitor;

/** Tracks the global stop condition of a simulation run. */
public final class StopCondition {
  private final int transition;
  private final int targetCount;
  private int firedCount;
  private boolean finished;

  public StopCondition(int transition, int targetCount) {
    if (transition < 0) {
      throw new IllegalArgumentException("Stop transition must be non-negative.");
    }
    if (targetCount <= 0) {
      throw new IllegalArgumentException("Stop count must be positive.");
    }
    this.transition = transition;
    this.targetCount = targetCount;
  }

  public boolean recordFire(int firedTransition) {
    if (finished || firedTransition != transition) {
      return false;
    }

    firedCount++;
    if (firedCount >= targetCount) {
      finished = true;
      return true;
    }
    return false;
  }
}
