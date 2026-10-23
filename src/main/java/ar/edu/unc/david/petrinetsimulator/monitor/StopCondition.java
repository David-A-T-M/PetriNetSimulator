package ar.edu.unc.david.petrinetsimulator.monitor;

/** Tracks the global stop condition of a simulation run. */
public final class StopCondition {
  private final int transition;
  private final int targetCount;
  private int firedCount;
  private boolean finished;

  /** Creates a stop condition for the given transition and target count. */
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

  /** Records a fire of the stop transition. Returns true if the stop condition is met. */
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
