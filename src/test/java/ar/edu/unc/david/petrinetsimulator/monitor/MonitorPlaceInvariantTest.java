package ar.edu.unc.david.petrinetsimulator.monitor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.unc.david.petrinetsimulator.config.logic.PlaceInvariantConfig;
import ar.edu.unc.david.petrinetsimulator.core.PetriNet;
import ar.edu.unc.david.petrinetsimulator.core.PetriNetMatrix;
import ar.edu.unc.david.petrinetsimulator.log.PetriLogger;
import ar.edu.unc.david.petrinetsimulator.policy.RandomPolicy;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests runtime place invariant enforcement inside Monitor.fireTransition. */
public class MonitorPlaceInvariantTest {

  @TempDir Path tempDir;

  private static PetriNet createSingleTransitionSelfLoopNet() {
    int[][] pre = {{1}};
    int[][] post = {{1}};

    return new PetriNet(
        new int[] {1}, new PetriNetMatrix(pre, post), new long[] {0}, new long[] {999_999});
  }

  @Test
  @DisplayName("fireTransition succeeds when configured place invariants hold")
  void fireTransitionSucceedsWhenInvariantsHold() throws Exception {
    PetriNet petriNet = createSingleTransitionSelfLoopNet();
    PetriLogger logger = new PetriLogger(tempDir.resolve("invariant-ok.log").toString());

    try {
      Monitor monitor =
          new Monitor(
              petriNet,
              new RandomPolicy(),
              logger,
              null,
              List.of(new PlaceInvariantConfig(new int[] {1}, 1)),
              true);

      assertTrue(monitor.fireTransition(0));
    } finally {
      logger.close();
    }
  }

  @Test
  @DisplayName("fireTransition throws when a configured place invariant is violated")
  void fireTransitionThrowsWhenInvariantsViolated() throws Exception {
    PetriNet petriNet = createSingleTransitionSelfLoopNet();
    PetriLogger logger = new PetriLogger(tempDir.resolve("invariant-fail.log").toString());

    try {
      Monitor monitor =
          new Monitor(
              petriNet,
              new RandomPolicy(),
              logger,
              null,
              List.of(new PlaceInvariantConfig(new int[] {1}, 0)),
              true);

      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> monitor.fireTransition(0));
      assertTrue(ex.getMessage().contains("Place invariant violation"));
    } finally {
      logger.close();
    }
  }

  @Test
  @DisplayName("fireTransition skips invariant checks when runtime flag is disabled")
  void fireTransitionSkipsInvariantChecksWhenFlagDisabled() throws Exception {
    PetriNet petriNet = createSingleTransitionSelfLoopNet();
    PetriLogger logger = new PetriLogger(tempDir.resolve("invariant-disabled.log").toString());

    try {
      Monitor monitor =
          new Monitor(
              petriNet,
              new RandomPolicy(),
              logger,
              null,
              List.of(new PlaceInvariantConfig(new int[] {1}, 0)),
              false);

      assertDoesNotThrow(() -> monitor.fireTransition(0));
      assertFalse(Thread.currentThread().isInterrupted());
    } finally {
      logger.close();
    }
  }
}
