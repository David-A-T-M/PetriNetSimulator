package ar.edu.unc.david.petrinetsimulator.monitor;

import ar.edu.unc.david.petrinetsimulator.core.PetriNet;
import ar.edu.unc.david.petrinetsimulator.log.PetriLogger;
import ar.edu.unc.david.petrinetsimulator.policy.Policy;
import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Monitor class responsible for controlling transitions in a Petri net according to a specified
 * policy while ensuring thread-safety and fair access to transitions.
 */
public class Monitor implements MonitorInterface {

  private final PetriNet petriNet;
  private final WaitingQueues queues;
  private final Policy policy;
  private final PetriLogger logger;
  private final StopCondition stopCondition;

  private final ReentrantLock lock = new ReentrantLock(true);
  private final Condition[] condVar;
  private boolean finished;

  /**
   * Initializes the Monitor with a given Petri net, policy for selecting transitions, and a logger
   * for recording events. The monitor sets up waiting queues for each transition and initializes
   * condition variables to manage thread synchronization when transitions are not enabled.
   *
   * @param petriNet the Petri net that the monitor will control.
   * @param policy the policy that determines which transition to fire when multiple is enabled.
   * @param logger the logger used to record waiting and firing events.
   */
  public Monitor(PetriNet petriNet, Policy policy, PetriLogger logger) {
    this(petriNet, policy, logger, null);
  }

  /**
   * Initializes the Monitor with a given Petri net, policy, logger, and optional stop condition.
   *
   * @param petriNet the Petri net that the monitor will control.
   * @param policy the policy that determines which transition to fire when multiple is enabled.
   * @param logger the logger used to record waiting and firing events.
   * @param stopCondition the global stop condition, or null if the run should not stop early.
   */
  public Monitor(
      PetriNet petriNet, Policy policy, PetriLogger logger, StopCondition stopCondition) {
    this.petriNet = petriNet;
    this.policy = policy;
    this.logger = logger;
    this.stopCondition = stopCondition;

    int numTransitions = petriNet.matrix().numTransitions();
    this.queues = new WaitingQueues(numTransitions);

    this.condVar = new Condition[numTransitions];
    for (int i = 0; i < numTransitions; i++) {
      condVar[i] = lock.newCondition();
    }
  }

  @Override
  public boolean fireTransition(int transition) {
    lock.lock();
    try {
      if (finished) {
        return false;
      }

      while (!finished
          && (!petriNet.isEnabled(transition) || !petriNet.isInTimeWindow(transition))) {

        if (petriNet.isExpired(transition)) {
          return false;
        }

        Thread current = Thread.currentThread();
        queues.enqueue(transition, current);
        logger.logWait(transition);

        if (!petriNet.isEnabled(transition)) {
          condVar[transition].await();
        } else {
          long waitNanos = petriNet.nanosUntilOpen(transition);
          condVar[transition].awaitNanos(waitNanos);
        }

        queues.remove(transition, current);
        if (finished) {
          return false;
        }
      }

      if (finished) {
        return false;
      }

      int[] markingBefore = petriNet.getMarking().clone();
      petriNet.fire(transition);
      petriNet.updateSensitizationTimestamps();
      int[] markingAfter = petriNet.getMarking();

      logger.logFire(transition, markingBefore, markingAfter);

      if (stopCondition != null && stopCondition.recordFire(transition)) {
        finished = true;
        signalAllWaitingThreads();
        return false;
      }

      wakeEligible();
      return true;

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    } finally {
      lock.unlock();
    }
  }

  private void wakeEligible() {
    if (finished) {
      return;
    }

    int[] enabled = petriNet.getEnabledTransitions();

    int[] inWindow = Arrays.stream(enabled).filter(petriNet::isInTimeWindow).toArray();

    int[] candidates = queues.getWaitingAmong(inWindow);

    if (candidates.length == 0) {
      return;
    }

    int chosen = policy.decide(candidates);
    if (chosen == -1) {
      return;
    }

    queues.dequeue(chosen);
    condVar[chosen].signal();
  }

  private void signalAllWaitingThreads() {
    for (Condition condition : condVar) {
      condition.signalAll();
    }
  }
}
