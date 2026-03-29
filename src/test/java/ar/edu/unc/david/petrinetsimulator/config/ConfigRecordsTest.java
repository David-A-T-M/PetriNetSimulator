package ar.edu.unc.david.petrinetsimulator.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.unc.david.petrinetsimulator.config.logic.AgentConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.LoggingConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.NetConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.PlaceInvariantConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.PolicyConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.RuntimeConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.SimulationConfig;
import ar.edu.unc.david.petrinetsimulator.config.logic.TransitionConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for config records: validations, nominal cases, and edge cases. */
class ConfigRecordsTest {

  @Nested
  @DisplayName("RuntimeConfig")
  class RuntimeConfigTests {

    @Test
    @DisplayName("accepts zero timeout")
    void acceptsZeroTimeout() {
      RuntimeConfig config = new RuntimeConfig(0, true);

      assertEquals(0, config.joinTimeoutMs());
      assertTrue(config.builtInValidation());
    }

    @Test
    @DisplayName("accepts positive timeout")
    void acceptsPositiveTimeout() {
      RuntimeConfig config = new RuntimeConfig(30_000, false);

      assertEquals(30_000, config.joinTimeoutMs());
      assertFalse(config.builtInValidation());
    }

    @Test
    @DisplayName("rejects negative timeout")
    void rejectsNegativeTimeout() {
      assertThrows(IllegalArgumentException.class, () -> new RuntimeConfig(-1, true));
    }
  }

  @Nested
  @DisplayName("PolicyConfig")
  class PolicyConfigTests {

    @Test
    @DisplayName("accepts RANDOM")
    void acceptsRandomUppercase() {
      assertDoesNotThrow(() -> new PolicyConfig("RANDOM"));
    }

    @Test
    @DisplayName("accepts random (case-insensitive)")
    void acceptsRandomLowercase() {
      PolicyConfig config = new PolicyConfig("random");
      assertEquals("random", config.type());
    }

    @Test
    @DisplayName("rejects null type")
    void rejectsNullType() {
      assertThrows(IllegalArgumentException.class, () -> new PolicyConfig(null));
    }

    @Test
    @DisplayName("rejects blank type")
    void rejectsBlankType() {
      assertThrows(IllegalArgumentException.class, () -> new PolicyConfig("   "));
    }

    @Test
    @DisplayName("rejects unsupported type")
    void rejectsUnsupportedType() {
      assertThrows(IllegalArgumentException.class, () -> new PolicyConfig("ROUND_ROBIN"));
    }
  }

  @Nested
  @DisplayName("TransitionConfig")
  class TransitionConfigTests {

    @Test
    @DisplayName("accepts valid timing config")
    void acceptsValidTransitionConfig() {
      TransitionConfig config = new TransitionConfig(0, 0L, 10L);

      assertEquals(0, config.id());
      assertEquals(0L, config.alpha());
      assertEquals(10L, config.beta());
    }

    @Test
    @DisplayName("accepts alpha == beta")
    void acceptsEqualBounds() {
      assertDoesNotThrow(() -> new TransitionConfig(1, 5L, 5L));
    }

    @Test
    @DisplayName("rejects negative id")
    void rejectsNegativeId() {
      assertThrows(IllegalArgumentException.class, () -> new TransitionConfig(-1, 0L, 10L));
    }

    @Test
    @DisplayName("rejects negative alpha")
    void rejectsNegativeAlpha() {
      assertThrows(IllegalArgumentException.class, () -> new TransitionConfig(0, -1L, 10L));
    }

    @Test
    @DisplayName("rejects negative beta")
    void rejectsNegativeBeta() {
      assertThrows(IllegalArgumentException.class, () -> new TransitionConfig(0, 1L, -1L));
    }

    @Test
    @DisplayName("rejects alpha greater than beta")
    void rejectsAlphaGreaterThanBeta() {
      assertThrows(IllegalArgumentException.class, () -> new TransitionConfig(0, 11L, 10L));
    }
  }

  @Nested
  @DisplayName("AgentConfig")
  class AgentConfigTests {

    @Test
    @DisplayName("accepts valid agent config")
    void acceptsValidAgentConfig() {
      AgentConfig config = new AgentConfig("P", 2, new int[] {0, 1, 2}, 100);

      assertEquals("P", config.namePrefix());
      assertEquals(2, config.count());
      assertArrayEquals(new int[] {0, 1, 2}, config.sequence());
      assertEquals(100, config.cycles());
    }

    @Test
    @DisplayName("rejects count == 0")
    void rejectsCountZero() {
      assertThrows(IllegalArgumentException.class, () -> new AgentConfig("P", 0, new int[] {0}, 1));
    }

    @Test
    @DisplayName("rejects count < 0")
    void rejectsCountNegative() {
      assertThrows(
          IllegalArgumentException.class, () -> new AgentConfig("P", -1, new int[] {0}, 1));
    }

    @Test
    @DisplayName("rejects cycles == 0")
    void rejectsCyclesZero() {
      assertThrows(IllegalArgumentException.class, () -> new AgentConfig("P", 1, new int[] {0}, 0));
    }

    @Test
    @DisplayName("rejects cycles < 0")
    void rejectsCyclesNegative() {
      assertThrows(
          IllegalArgumentException.class, () -> new AgentConfig("P", 1, new int[] {0}, -5));
    }

    @Test
    @DisplayName("rejects null namePrefix")
    void rejectsNullNamePrefix() {
      assertThrows(
          IllegalArgumentException.class, () -> new AgentConfig(null, 1, new int[] {0, 1}, 10));
    }

    @Test
    @DisplayName("rejects blank namePrefix")
    void rejectsBlankNamePrefix() {
      assertThrows(
          IllegalArgumentException.class, () -> new AgentConfig("   ", 1, new int[] {0, 1}, 10));
    }

    @Test
    @DisplayName("rejects null sequence")
    void rejectsNullSequence() {
      assertThrows(IllegalArgumentException.class, () -> new AgentConfig("P", 1, null, 10));
    }

    @Test
    @DisplayName("rejects empty sequence")
    void rejectsEmptySequence() {
      assertThrows(IllegalArgumentException.class, () -> new AgentConfig("P", 1, new int[] {}, 10));
    }
  }

  @Nested
  @DisplayName("NetConfig")
  class NetConfigTests {

    @Test
    @DisplayName("accepts valid matrices, initial marking, and transitions")
    void acceptsValidNetConfig() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {
        {0, 1},
        {1, 0}
      };
      int[] initial = {1, 0};
      List<TransitionConfig> transitions =
          List.of(new TransitionConfig(0, 0L, 10L), new TransitionConfig(1, 5L, 15L));

      NetConfig config = new NetConfig(pre, post, initial, transitions);

      assertArrayEquals(pre, config.pre());
      assertArrayEquals(post, config.post());
      assertArrayEquals(initial, config.initialMarking());
      assertEquals(2, config.transitions().size());
    }

    @Test
    @DisplayName("rejects empty pre matrix")
    void rejectsEmptyPre() {
      int[][] pre = {};
      int[][] post = {{0}};
      int[] initial = {1};
      List<TransitionConfig> transitions = List.of(new TransitionConfig(0, 0L, 10L));

      assertThrows(
          IllegalArgumentException.class, () -> new NetConfig(pre, post, initial, transitions));
    }

    @Test
    @DisplayName("rejects empty post matrix")
    void rejectsEmptyPost() {
      int[][] pre = {{1}};
      int[][] post = {};
      int[] initial = {1};
      List<TransitionConfig> transitions = List.of(new TransitionConfig(0, 0L, 10L));

      assertThrows(
          IllegalArgumentException.class, () -> new NetConfig(pre, post, initial, transitions));
    }

    @Test
    @DisplayName("rejects different place count between pre and post")
    void rejectsDifferentPlaceCount() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {{0, 1}};
      int[] initial = {1, 0};
      List<TransitionConfig> transitions =
          List.of(new TransitionConfig(0, 0L, 10L), new TransitionConfig(1, 5L, 15L));

      assertThrows(
          IllegalArgumentException.class, () -> new NetConfig(pre, post, initial, transitions));
    }

    @Test
    @DisplayName("rejects initialMarking size different from places")
    void rejectsDifferentInitialMarkingSize() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {
        {0, 1},
        {1, 0}
      };
      int[] initial = {1};
      List<TransitionConfig> transitions =
          List.of(new TransitionConfig(0, 0L, 10L), new TransitionConfig(1, 5L, 15L));

      assertThrows(
          IllegalArgumentException.class, () -> new NetConfig(pre, post, initial, transitions));
    }

    @Test
    @DisplayName("rejects different transition count between pre and post")
    void rejectsDifferentTransitionCount() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {
        {0, 1, 0},
        {1, 0, 1}
      };
      int[] initial = {1, 0};
      List<TransitionConfig> transitions =
          List.of(new TransitionConfig(0, 0L, 10L), new TransitionConfig(1, 5L, 15L));

      assertThrows(
          IllegalArgumentException.class, () -> new NetConfig(pre, post, initial, transitions));
    }

    @Test
    @DisplayName("rejects null transitions list")
    void rejectsNullTransitions() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {
        {0, 1},
        {1, 0}
      };
      int[] initial = {1, 0};

      assertThrows(IllegalArgumentException.class, () -> new NetConfig(pre, post, initial, null));
    }

    @Test
    @DisplayName("rejects transitions size mismatch")
    void rejectsTransitionsSizeMismatch() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {
        {0, 1},
        {1, 0}
      };
      int[] initial = {1, 0};
      List<TransitionConfig> transitions = List.of(new TransitionConfig(0, 0L, 10L));

      assertThrows(
          IllegalArgumentException.class, () -> new NetConfig(pre, post, initial, transitions));
    }

    @Test
    @DisplayName("rejects transition id out of bounds")
    void rejectsTransitionIdOutOfBounds() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {
        {0, 1},
        {1, 0}
      };
      int[] initial = {1, 0};
      List<TransitionConfig> transitions =
          List.of(new TransitionConfig(0, 0L, 10L), new TransitionConfig(2, 5L, 15L));

      assertThrows(
          IllegalArgumentException.class, () -> new NetConfig(pre, post, initial, transitions));
    }

    @Test
    @DisplayName("rejects when some transition id is missing")
    void rejectsMissingTransitionId() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {
        {0, 1},
        {1, 0}
      };
      int[] initial = {1, 0};
      List<TransitionConfig> transitions =
          List.of(new TransitionConfig(0, 0L, 10L), new TransitionConfig(0, 5L, 15L));

      assertThrows(
          IllegalArgumentException.class, () -> new NetConfig(pre, post, initial, transitions));
    }

    @Test
    @DisplayName("current behavior: null pre throws NullPointerException")
    void nullPreThrowsNpe() {
      int[][] post = {{0}};
      int[] initial = {0};
      List<TransitionConfig> transitions = List.of(new TransitionConfig(0, 0L, 10L));

      assertThrows(
          NullPointerException.class, () -> new NetConfig(null, post, initial, transitions));
    }

    @Test
    @DisplayName("current behavior: null post throws NullPointerException")
    void nullPostThrowsNpe() {
      int[][] pre = {{1}};
      int[] initial = {0};
      List<TransitionConfig> transitions = List.of(new TransitionConfig(0, 0L, 10L));

      assertThrows(
          NullPointerException.class, () -> new NetConfig(pre, null, initial, transitions));
    }

    @Test
    @DisplayName("current behavior: null initialMarking throws NullPointerException")
    void nullInitialMarkingThrowsNpe() {
      int[][] pre = {{1}};
      int[][] post = {{0}};
      List<TransitionConfig> transitions = List.of(new TransitionConfig(0, 0L, 10L));

      assertThrows(NullPointerException.class, () -> new NetConfig(pre, post, null, transitions));
    }
  }

  @Nested
  @DisplayName("PlaceInvariantConfig")
  class PlaceInvariantConfigTests {

    @Test
    @DisplayName("accepts valid places and non-negative constant")
    void acceptsValidInvariant() {
      PlaceInvariantConfig config = new PlaceInvariantConfig(new int[] {0, 1, 2}, 3);

      assertArrayEquals(new int[] {0, 1, 2}, config.places());
      assertEquals(3, config.constant());
    }

    @Test
    @DisplayName("rejects null places")
    void rejectsNullPlaces() {
      assertThrows(IllegalArgumentException.class, () -> new PlaceInvariantConfig(null, 1));
    }

    @Test
    @DisplayName("rejects empty places")
    void rejectsEmptyPlaces() {
      assertThrows(IllegalArgumentException.class, () -> new PlaceInvariantConfig(new int[] {}, 1));
    }

    @Test
    @DisplayName("rejects negative place entry")
    void rejectsNegativePlaceEntry() {
      assertThrows(
          IllegalArgumentException.class, () -> new PlaceInvariantConfig(new int[] {0, -1, 2}, 1));
    }

    @Test
    @DisplayName("rejects negative constant")
    void rejectsNegativeConstant() {
      assertThrows(
          IllegalArgumentException.class, () -> new PlaceInvariantConfig(new int[] {0, 1}, -1));
    }
  }

  @Nested
  @DisplayName("LoggingConfig")
  class LoggingConfigTests {

    @Test
    @DisplayName("stores values as provided")
    void storesValuesAsProvided() {
      LoggingConfig config = new LoggingConfig("petri_log.csv", true);

      assertEquals("petri_log.csv", config.file());
      assertTrue(config.logWaitWake());
    }

    @Test
    @DisplayName("current behavior: allows null file")
    void allowsNullFileCurrently() {
      LoggingConfig config = new LoggingConfig(null, false);

      assertNull(config.file());
      assertFalse(config.logWaitWake());
    }
  }

  @Nested
  @DisplayName("SimulationConfig")
  class SimulationConfigTests {

    @Test
    @DisplayName("stores all nested config sections")
    void storesAllSections() {
      SimulationConfig config =
          new SimulationConfig(
              new NetConfig(
                  new int[][] {
                    {1, 0},
                    {0, 1}
                  },
                  new int[][] {
                    {0, 1},
                    {1, 0}
                  },
                  new int[] {1, 0},
                  List.of(new TransitionConfig(0, 0L, 10L), new TransitionConfig(1, 5L, 15L))),
              List.of(new PlaceInvariantConfig(new int[] {1, 1}, 1)),
              List.of(new AgentConfig("P", 1, new int[] {0}, 1)),
              new LoggingConfig("petri_log.csv", false),
              new RuntimeConfig(10_000, true),
              new PolicyConfig("RANDOM"));

      assertEquals(2, config.net().pre().length);
      assertEquals(1, config.placeInvariants().size());
      assertEquals(1, config.agents().size());
      assertEquals("petri_log.csv", config.logging().file());
      assertEquals(10_000, config.runtime().joinTimeoutMs());
      assertEquals("RANDOM", config.policy().type());
    }

    @Test
    @DisplayName("current behavior: allows null sections")
    void allowsNullSectionsCurrently() {
      SimulationConfig config = new SimulationConfig(null, null, null, null, null, null);

      assertNull(config.net());
      assertNull(config.placeInvariants());
      assertNull(config.agents());
      assertNull(config.logging());
      assertNull(config.runtime());
      assertNull(config.policy());
    }
  }
}
