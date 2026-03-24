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

  private final ReentrantLock lock = new ReentrantLock(true);
  private final Condition[] condVar;

  /**
   * Initializes the Monitor with a given Petri net and a policy for selecting transitions. The
   * monitor sets up waiting queues for each transition and initializes condition variables to
   * manage thread synchronization when transitions are not enabled.
   *
   * @param petriNet the Petri net that the monitor will control.
   * @param policy the policy that determines which transition to fire when multiple is enabled.
   */
  public Monitor(PetriNet petriNet, Policy policy) {
    this.petriNet = petriNet;
    this.policy = policy;

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

      petriNet.fire(transition);

      return true;

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    } finally {
      lock.unlock();
    }
  }
}
