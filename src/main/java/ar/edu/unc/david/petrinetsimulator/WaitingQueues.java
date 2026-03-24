package ar.edu.unc.david.petrinetsimulator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Manages waiting queues for each transition in a Petri net. Each transition has its own queue of
 * threads that are waiting to fire that transition.
 */
public class WaitingQueues {

  private final Map<Integer, Queue<Thread>> waitMap;
  private final Set<Thread> threadToTransition;

  /**
   * Initializes the waiting queues for a given number of transitions. Each transition is associated
   * with an empty queue of waiting threads.
   *
   * @param transitionCount the total number of transitions in the Petri net, used to initialize the
   *     queues for each transition.
   */
  public WaitingQueues(int transitionCount) {
    if (transitionCount <= 0) {
      throw new IllegalArgumentException("Transition count must be positive.");
    }

    threadToTransition = new HashSet<>();
    waitMap = new HashMap<>();
    for (int t = 0; t < transitionCount; t++) {
      waitMap.put(t, new LinkedList<>());
    }
  }

  void enqueue(int transition, Thread thread) {
    java.util.Objects.requireNonNull(thread, "Thread cannot be null");
    Queue<Thread> queue = waitMap.get(transition);

    if (queue == null) {
      throw new IndexOutOfBoundsException("Invalid transition: " + transition);
    }

    if (queue.contains(thread)) {
      throw new IllegalStateException("Thread is already waiting for transition " + transition);
    }

    if (threadToTransition.contains(thread)) {
      throw new IllegalStateException("Thread is already waiting for another transition");
    }

    queue.add(thread);
    threadToTransition.add(thread);
  }

  /** Dequeues a thread from the waiting queue of the specified transition. */
  Thread dequeue(int transition) {
    Queue<Thread> queue = waitMap.get(transition);
    if (queue == null) {
      throw new IndexOutOfBoundsException("Invalid transition: " + transition);
    }

    Thread thread = queue.poll();
    if (thread != null) {
      threadToTransition.remove(thread);
    }
    return thread;
  }

  boolean hasWaiting(int transition) {
    Queue<Thread> queue = waitMap.get(transition);
    if (queue == null) {
      throw new IndexOutOfBoundsException("Invalid transition: " + transition);
    }
    return !queue.isEmpty();
  }

  /** Returns an array of transitions from the input that currently have waiting threads. */
  int[] getWaitingAmong(int[] enabledTransitions) {
    return Arrays.stream(enabledTransitions).filter(this::hasWaiting).toArray();
  }
}
