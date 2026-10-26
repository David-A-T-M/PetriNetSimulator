package ar.edu.unc.david.petrinetsimulator.monitor;

/** Tracks the global stop condition of a simulation run. */
public final class StopCondition {
  private final int transition;
  private final int targetCount;
  private final int admissionTransition;
  private int firedCount;
  private int admittedCount;
  private volatile boolean finished;

  /** Creates a stop condition for the given transition and target count. */
  public StopCondition(int transition, int targetCount) {
    this(transition, targetCount, -1);
  }

  /** Creates a stop condition for the given transition, target count, and admission transition. */
  public StopCondition(int transition, int targetCount, int admissionTransition) {
    if (transition < 0) {
      throw new IllegalArgumentException("Stop transition must be non-negative.");
    }
    if (targetCount <= 0) {
      throw new IllegalArgumentException("Stop count must be positive.");
    }
    if (admissionTransition < -1) {
      throw new IllegalArgumentException("Admission transition must be -1 or non-negative.");
    }
    this.transition = transition;
    this.targetCount = targetCount;
    this.admissionTransition = admissionTransition;
  }

  /** Records a fire of the stop transition. Returns true if the stop condition is met. */
  public boolean recordFire(int firedTransition) {
    if (admissionTransition >= 0 && firedTransition == admissionTransition) {
      admittedCount++;
    }

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

  /** Returns true if the requested transition is rejected due to the stop condition. */
  public boolean rejects(int requestedTransition) {
    return admissionTransition >= 0
        && requestedTransition == admissionTransition
        && admittedCount >= targetCount;
  }

  public boolean isReached() {
    return finished;
  }
}
