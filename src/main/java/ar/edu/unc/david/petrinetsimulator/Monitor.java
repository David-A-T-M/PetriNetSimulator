package ar.edu.unc.david.petrinetsimulator;

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

  private final ReentrantLock lock = new ReentrantLock(true);
  private final Condition[] condVar;

  /**
   * Initializes the Monitor with a given Petri net, policy for selecting transitions, and a logger
   * for recording events. The monitor sets up waiting queues for each transition and initializes
   * condition variables to manage thread synchronization when transitions are not enabled.
   *
   * @param petriNet the Petri net that the monitor will control.
   * @param policy the policy that determines which transition to fire when multiple is enabled.
   */
  public Monitor(PetriNet petriNet, Policy policy, PetriLogger logger) {
    this.petriNet = petriNet;
    this.policy = policy;
    this.logger = logger;

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
      while (!petriNet.isEnabled(transition)) {
        Thread current = Thread.currentThread();
        queues.enqueue(transition, current);

        condVar[transition].await();
      }

      int[] markingBefore = petriNet.getMarking().clone();
      petriNet.fire(transition);
      int[] markingAfter = petriNet.getMarking();

      logger.logFire(transition, markingBefore, markingAfter);

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
    int[] enabled = petriNet.getEnabledTransitions();
    int[] candidates = queues.getWaitingAmong(enabled);

    if (candidates.length == 0) {
      return;
    }

    int chosen = policy.decide(candidates);
    if (chosen == -1) {
      return;
    }

    Thread toWake = queues.dequeue(chosen);

    if (toWake != null) {

      condVar[chosen].signal();
    }
  }
}
