package ar.edu.unc.david.petrinetsimulator.monitor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.unc.david.petrinetsimulator.core.PetriNet;
import ar.edu.unc.david.petrinetsimulator.core.PetriNetMatrix;
import ar.edu.unc.david.petrinetsimulator.log.PetriLogger;
import ar.edu.unc.david.petrinetsimulator.policy.RandomPolicy;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Integration tests for temporal waiting strategy in Monitor. */
public class MonitorTemporalWaitingStrategyTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("temporal waiting happens outside monitor queues")
  void temporalWaitingHappensOutsideMonitorQueues() throws Exception {
    PetriNet petriNet = createSingleTransitionSelfLoopNet(400L, 999_999L);
    PetriLogger logger = new PetriLogger(tempDir.resolve("temporal_outside.log").toString());

    try {
      Monitor monitor = new Monitor(petriNet, new RandomPolicy(), logger);
      WaitingQueues queues = extractQueues(monitor);

      AtomicBoolean fired = new AtomicBoolean(false);
      Thread thread = new Thread(() -> fired.set(monitor.fireTransition(0)), "temporal-waiter");

      assertTimeoutPreemptively(
          Duration.ofSeconds(3),
          () -> {
            thread.start();

            Thread.sleep(120L);

            assertFalse(queues.hasWaiting(0));

            thread.join(2_000L);
            assertFalse(thread.isAlive());
            assertTrue(fired.get());
          });
    } finally {
      logger.close();
    }
  }

  private static PetriNet createSingleTransitionSelfLoopNet(long alpha, long beta) {
    int[][] pre = {{1}};
    int[][] post = {{1}};

    return new PetriNet(
        new int[] {1}, new PetriNetMatrix(pre, post), new long[] {alpha}, new long[] {beta});
  }

  private static WaitingQueues extractQueues(Monitor monitor) throws ReflectiveOperationException {
    Field queuesField = Monitor.class.getDeclaredField("queues");
    queuesField.setAccessible(true);
    return (WaitingQueues) queuesField.get(monitor);
  }
}
