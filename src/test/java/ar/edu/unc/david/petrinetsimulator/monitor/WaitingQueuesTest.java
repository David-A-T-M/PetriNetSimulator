package ar.edu.unc.david.petrinetsimulator.monitor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for the WaitingQueues class. */
public class WaitingQueuesTest {

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor initializes all queues as empty")
    void constructor_initializesAllQueuesAsEmpty() {
      WaitingQueues queues = new WaitingQueues(3);

      assertFalse(queues.hasWaiting(0));
      assertFalse(queues.hasWaiting(1));
      assertFalse(queues.hasWaiting(2));
    }

    @Test
    @DisplayName("Constructor with zero transitions throws")
    void constructor_withZeroTransitions_Throws() {
      assertThrows(IllegalArgumentException.class, () -> new WaitingQueues(0));
    }

    @Test
    @DisplayName("Constructor with negative transitions throws")
    void constructor_withNegativeTransitions_Throws() {
      assertThrows(IllegalArgumentException.class, () -> new WaitingQueues(-1));
    }
  }

  @Nested
  @DisplayName("enqueue/dequeue behavior")
  class EnqueueDequeueTests {

    @Test
    @DisplayName("dequeue returns null when queue is empty")
    void dequeue_returnsNullWhenQueueIsEmpty() {
      WaitingQueues queues = new WaitingQueues(1);

      assertNull(queues.dequeue(0));
      assertFalse(queues.hasWaiting(0));
    }

    @Test
    @DisplayName("enqueue then dequeue returns same thread")
    void enqueue_thenDequeue_returnsSameThread() {
      WaitingQueues queues = new WaitingQueues(1);
      Thread t = new Thread(() -> {});

      queues.enqueue(0, t);

      assertTrue(queues.hasWaiting(0));
      assertSame(queues.dequeue(0), t);
      assertFalse(queues.hasWaiting(0));
    }

    @Test
    @DisplayName("dequeue respects FIFO order")
    void dequeue_respectsFifoOrder() {
      WaitingQueues queues = new WaitingQueues(1);
      Thread t1 = new Thread(() -> {});
      Thread t2 = new Thread(() -> {});

      queues.enqueue(0, t1);
      queues.enqueue(0, t2);

      assertSame(queues.dequeue(0), t1);
      assertSame(queues.dequeue(0), t2);
      assertNull(queues.dequeue(0));
      assertFalse(queues.hasWaiting(0));
    }

    @Test
    @DisplayName("queues are independent per transition")
    void queues_areIndependentPerTransition() {
      WaitingQueues queues = new WaitingQueues(2);
      Thread t0 = new Thread(() -> {});
      Thread t1 = new Thread(() -> {});

      queues.enqueue(0, t0);
      queues.enqueue(1, t1);

      assertTrue(queues.hasWaiting(0));
      assertTrue(queues.hasWaiting(1));

      assertSame(queues.dequeue(0), t0);
      assertFalse(queues.hasWaiting(0));
      assertTrue(queues.hasWaiting(1));

      assertSame(queues.dequeue(1), t1);
      assertFalse(queues.hasWaiting(1));
    }

    @Test
    @DisplayName("enqueue throws NullPointerException when thread is null")
    void enqueue_throwsNullPointerExceptionWhenThreadIsNull() {
      WaitingQueues queues = new WaitingQueues(1);

      assertThrows(NullPointerException.class, () -> queues.enqueue(0, null));
    }

    @Test
    @DisplayName("enqueue throws when same thread is already queued for the transition")
    void enqueue_throwsIllegalStateExceptionWhenSameThreadAlreadyQueuedForSameTransition() {
      WaitingQueues queues = new WaitingQueues(1);
      Thread t = new Thread(() -> {});

      queues.enqueue(0, t);

      assertThrows(IllegalStateException.class, () -> queues.enqueue(0, t));
    }

    @Test
    @DisplayName("dequeue then enqueue same thread again is allowed")
    void dequeue_thenEnqueueSameThreadAgain_isAllowed() {
      WaitingQueues queues = new WaitingQueues(1);
      Thread t = new Thread(() -> {});

      queues.enqueue(0, t);
      assertSame(t, queues.dequeue(0));

      assertDoesNotThrow(() -> queues.enqueue(0, t));
      assertSame(t, queues.dequeue(0));
    }

    @Test
    @DisplayName("same thread cannot be queued in different transitions simultaneously")
    void sameThread_cannotBeQueuedInDifferentTransitions() {
      WaitingQueues queues = new WaitingQueues(2);
      Thread t = new Thread(() -> {});

      queues.enqueue(0, t);

      assertThrows(IllegalStateException.class, () -> queues.enqueue(1, t));
    }
  }

  @Nested
  @DisplayName("getWaitingAmong")
  class GetWaitingAmongTests {

    @Test
    @DisplayName("getWaitingAmong returns only enabled transitions that have waiting threads")
    void getWaitingAmong_returnsOnlyEnabledWithWaiting() {
      WaitingQueues queues = new WaitingQueues(4);

      queues.enqueue(1, new Thread(() -> {}));
      queues.enqueue(3, new Thread(() -> {}));

      assertArrayEquals(new int[] {1, 3}, queues.getWaitingAmong(new int[] {0, 1, 2, 3}));
    }

    @Test
    @DisplayName("getWaitingAmong preserves input order")
    void getWaitingAmong_preservesInputOrder() {
      WaitingQueues queues = new WaitingQueues(4);

      queues.enqueue(1, new Thread(() -> {}));
      queues.enqueue(3, new Thread(() -> {}));

      assertArrayEquals(new int[] {3, 1}, queues.getWaitingAmong(new int[] {3, 2, 1, 0}));
    }

    @Test
    @DisplayName("getWaitingAmong preserves duplicates from input")
    void getWaitingAmong_preservesDuplicatesFromInput() {
      WaitingQueues queues = new WaitingQueues(3);

      queues.enqueue(1, new Thread(() -> {}));

      assertArrayEquals(new int[] {1, 1, 1}, queues.getWaitingAmong(new int[] {1, 1, 1}));
    }

    @Test
    @DisplayName("getWaitingAmong returns empty array for empty input")
    void getWaitingAmong_returnsEmptyForEmptyInput() {
      WaitingQueues queues = new WaitingQueues(2);
      queues.enqueue(0, new Thread(() -> {}));

      assertArrayEquals(new int[0], queues.getWaitingAmong(new int[0]));
    }

    @Test
    @DisplayName("getWaitingAmong throws NullPointerException when input array is null")
    void getWaitingAmong_throwsWhenInputIsNull() {
      WaitingQueues queues = new WaitingQueues(1);

      assertThrows(NullPointerException.class, () -> queues.getWaitingAmong(null));
    }
  }

  @Nested
  @DisplayName("Invalid transition indexes")
  class InvalidTransitionIndexesTests {

    @Test
    @DisplayName("hasWaiting throws IndexOutOfBoundsException for out-of-range transition")
    void hasWaiting_throwsForOutOfRangeTransition() {
      WaitingQueues queues = new WaitingQueues(2);

      assertThrows(IndexOutOfBoundsException.class, () -> queues.hasWaiting(-1));
      assertThrows(IndexOutOfBoundsException.class, () -> queues.hasWaiting(2));
    }

    @Test
    @DisplayName("enqueue throws IndexOutOfBoundsException for invalid transition")
    void enqueue_throwsForInvalidTransition() {
      WaitingQueues queues = new WaitingQueues(2);

      assertThrows(IndexOutOfBoundsException.class, () -> queues.enqueue(-1, new Thread(() -> {})));
      assertThrows(IndexOutOfBoundsException.class, () -> queues.enqueue(2, new Thread(() -> {})));
    }

    @Test
    @DisplayName("dequeue throws IndexOutOfBoundsException for invalid transition")
    void dequeue_throwsForInvalidTransition() {
      WaitingQueues queues = new WaitingQueues(2);

      assertThrows(IndexOutOfBoundsException.class, () -> queues.dequeue(-1));
      assertThrows(IndexOutOfBoundsException.class, () -> queues.dequeue(2));
    }

    @Test
    @DisplayName(
        "getWaitingAmong throws IndexOutOfBoundsException when input contains invalid transition")
    void getWaitingAmong_throwsWhenInputContainsInvalidTransition() {
      WaitingQueues queues = new WaitingQueues(2);
      queues.enqueue(1, new Thread(() -> {}));

      assertThrows(IndexOutOfBoundsException.class, () -> queues.getWaitingAmong(new int[] {0, 2}));
      assertThrows(
          IndexOutOfBoundsException.class, () -> queues.getWaitingAmong(new int[] {-1, 1}));
    }
  }
}
