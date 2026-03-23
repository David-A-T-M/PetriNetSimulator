package ar.edu.unc.david.petrinetsimulator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for the PetriNet class. */
public class PetriNetTest {
  private PetriNet createSimpleNet(int[] initialMarking) {
    int[][] pre = {
      {1, 0},
      {0, 1}
    };

    int[][] post = {
      {0, 1},
      {1, 0}
    };

    PetriNetMatrix matrix = new PetriNetMatrix(pre, post);
    return new PetriNet(initialMarking, matrix);
  }

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor throws NullPointerException when initial marking is null")
    void constructor_throwsNullPointerExceptionWhenInitialMarkingIsNull() {
      assertThrows(
          NullPointerException.class,
          () -> new PetriNet(null, new PetriNetMatrix(new int[1][1], new int[1][1])));
    }

    @Test
    @DisplayName("Constructor throws NullPointerException when PetriNetMatrix is null")
    void constructor_throwsNullPointerExceptionWhenPetriNetMatrixIsNull() {
      assertThrows(NullPointerException.class, () -> new PetriNet(new int[1], null));
    }

    @Test
    @DisplayName("Constructor throws when initial marking is shorter than number of places")
    void constructor_throwsIllegalArgumentExceptionWhenInitialMarkingIsShorterThanPlaces() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {
        {0, 1},
        {1, 0}
      };
      PetriNetMatrix matrix = new PetriNetMatrix(pre, post);

      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new PetriNet(new int[] {1}, matrix));

      assertTrue(ex.getMessage().contains("Initial marking length"));
    }

    @Test
    @DisplayName("Constructor throws when initial marking is longer than number of places")
    void constructor_throwsIllegalArgumentExceptionWhenInitialMarkingIsLongerThanPlaces() {
      int[][] pre = {
        {1, 0},
        {0, 1}
      };
      int[][] post = {
        {0, 1},
        {1, 0}
      };
      PetriNetMatrix matrix = new PetriNetMatrix(pre, post);

      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> new PetriNet(new int[] {1, 0, 5}, matrix));

      assertTrue(ex.getMessage().contains("Initial marking length"));
    }

    @Test
    @DisplayName("Constructor initializes marking correctly")
    void constructor_initializesMarking() {
      int[] initial = {1, 0};
      PetriNet net = createSimpleNet(initial);
      assertArrayEquals(initial, net.getMarking());
    }

    @Test
    @DisplayName("Constructor should defensively copy initial marking (regression test)")
    void constructor_shouldDefensivelyCopyInitialMarking_regression() {
      int[] initial = {1, 0};
      PetriNet net = createSimpleNet(initial);

      initial[0] = 99;

      assertArrayEquals(new int[] {1, 0}, net.getMarking());
    }
  }

  @Nested
  @DisplayName("isEnabled method")
  class IsEnabledTests {

    @Test
    @DisplayName("Transition is enabled when marking has enough tokens")
    void isEnabled_returnsTrueWhenTransitionHasEnoughTokens() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      assertTrue(net.isEnabled(0));
    }

    @Test
    @DisplayName("Transition is not enabled when marking does not have enough tokens")
    void isEnabled_returnsFalseWhenTransitionDoesNotHaveEnoughTokens() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      assertFalse(net.isEnabled(1));
    }

    @Test
    void getEnabledTransitions_returnsAllWhenAllAreEnabled() {
      PetriNet net = createSimpleNet(new int[] {1, 1});
      assertArrayEquals(new int[] {0, 1}, net.getEnabledTransitions());
    }

    @Test
    void isEnabled_reflectsStateAfterFire() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      assertTrue(net.isEnabled(0));
      assertFalse(net.isEnabled(1));

      net.fire(0);

      assertFalse(net.isEnabled(0));
      assertTrue(net.isEnabled(1));
    }
  }

  @Nested
  @DisplayName("getEnabledTransitions method")
  class GetEnabledTransitionsTests {

    @Test
    @DisplayName("getEnabledTransitions returns the correct list of enabled transitions")
    void getEnabledTransitions_returnsExpectedTransitions() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      assertArrayEquals(new int[] {0}, net.getEnabledTransitions());

      net.fire(0);

      assertArrayEquals(new int[] {1}, net.getEnabledTransitions());
    }

    @Test
    void getEnabledTransitions_returnsAllWhenAllAreEnabled() {
      PetriNet net = createSimpleNet(new int[] {1, 1});
      assertArrayEquals(new int[] {0, 1}, net.getEnabledTransitions());
    }

    @Test
    void getEnabledTransitions_returnsEmptyWhenNoneAreEnabled() {
      PetriNet net = createSimpleNet(new int[] {0, 0});
      assertArrayEquals(new int[0], net.getEnabledTransitions());
    }
  }

  @Nested
  @DisplayName("fire method")
  class FireTests {

    @Test
    @DisplayName("Firing a transition updates the marking according to the incidence matrix")
    void fire_updatesMarkingUsingIncidenceColumn() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      net.fire(0);

      assertArrayEquals(new int[] {0, 1}, net.getMarking());
    }

    @Test
    @DisplayName("Firing a disabled transition throws IllegalStateException")
    void fire_onDisabledTransition_throwsIllegalStateException() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      assertThrows(IllegalStateException.class, () -> net.fire(1));
    }

    @Test
    @DisplayName("Firing a disabled transition does not mutate marking")
    void fire_onDisabledTransition_doesNotMutateMarking() {
      PetriNet net = createSimpleNet(new int[] {1, 0});
      int[] before = net.getMarking();

      assertThrows(IllegalStateException.class, () -> net.fire(1));

      assertArrayEquals(before, net.getMarking());
    }

    @Test
    @DisplayName("After failed fire, enabled transitions remain unchanged")
    void fire_onDisabledTransition_doesNotChangeEnabledTransitions() {
      PetriNet net = createSimpleNet(new int[] {1, 0});
      int[] enabledBefore = net.getEnabledTransitions();

      assertThrows(IllegalStateException.class, () -> net.fire(1));

      assertArrayEquals(enabledBefore, net.getEnabledTransitions());
    }

    @Test
    @DisplayName("After failed fire, a valid enabled transition can still fire")
    void fire_afterFailedAttempt_canStillFireEnabledTransition() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      assertThrows(IllegalStateException.class, () -> net.fire(1));
      net.fire(0);

      assertArrayEquals(new int[] {0, 1}, net.getMarking());
    }

    @Test
    @DisplayName("Initial marking is restored after firing a transition and then its inverse")
    void fire_sequence_t0ThenT1_restoresInitialMarking() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      net.fire(0);
      net.fire(1);

      assertArrayEquals(new int[] {1, 0}, net.getMarking());
    }
  }

  @Test
  @DisplayName("getMarking returns a defensive copy of the marking array")
  void getMarking_returnsDefensiveCopy() {
    PetriNet net = createSimpleNet(new int[] {1, 0});

    int[] copy = net.getMarking();
    copy[0] = 99;

    assertArrayEquals(new int[] {1, 0}, net.getMarking());
  }

  @Nested
  @DisplayName("getTokens method")
  class GetTokensTests {

    @Test
    @DisplayName("getTokens returns correct value for first place")
    void getTokens_returnsValueForFirstPlace() {
      PetriNet net = createSimpleNet(new int[] {3, 7});

      assertEquals(3, net.getTokens(0));
    }

    @Test
    @DisplayName("getTokens returns correct value for last place")
    void getTokens_returnsValueForLastPlace() {
      PetriNet net = createSimpleNet(new int[] {3, 7});

      assertEquals(7, net.getTokens(1));
    }

    @Test
    @DisplayName("getTokens reflects state after a fire")
    void getTokens_reflectsStateAfterFire() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      net.fire(0);

      assertEquals(0, net.getTokens(0));
      assertEquals(1, net.getTokens(1));
    }

    @Test
    @DisplayName("getTokens throws IndexOutOfBoundsException for negative index")
    void getTokens_throwsIndexOutOfBoundsExceptionForNegativeIndex() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      IndexOutOfBoundsException ex =
          assertThrows(IndexOutOfBoundsException.class, () -> net.getTokens(-1));

      assertTrue(ex.getMessage().contains("-1"));
    }

    @Test
    @DisplayName("getTokens throws IndexOutOfBoundsException for index equal to number of places")
    void getTokens_throwsIndexOutOfBoundsExceptionForIndexEqualToNumPlaces() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      IndexOutOfBoundsException ex =
          assertThrows(IndexOutOfBoundsException.class, () -> net.getTokens(2));

      assertTrue(ex.getMessage().contains("2"));
    }

    @Test
    @DisplayName(
        "getTokens throws IndexOutOfBoundsException for index greater than number of places")
    void getTokens_throwsIndexOutOfBoundsExceptionForIndexGreaterThanNumPlaces() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      assertThrows(IndexOutOfBoundsException.class, () -> net.getTokens(99));
    }
  }

  @Nested
  @DisplayName("checkPlacesInvariant method")
  class CheckPlacesInvariantTests {

    @Test
    @DisplayName("checkPlacesInvariant returns true when weighted sum equals expected constant")
    void checkPlacesInvariant_returnsTrueWhenSumMatchesExpectedConstant() {
      PetriNet net = createSimpleNet(new int[] {2, 3});

      assertTrue(net.checkPlacesInvariant(new int[] {1, 2}, 8)); // 1*2 + 2*3 = 8
    }

    @Test
    @DisplayName(
        "checkPlacesInvariant returns false when weighted sum does not equal expected constant")
    void checkPlacesInvariant_returnsFalseWhenSumDoesNotMatchExpectedConstant() {
      PetriNet net = createSimpleNet(new int[] {2, 3});

      assertFalse(net.checkPlacesInvariant(new int[] {1, 2}, 7));
    }

    @Test
    @DisplayName("checkPlacesInvariant supports negative coefficients")
    void checkPlacesInvariant_supportsNegativeCoefficients() {
      PetriNet net = createSimpleNet(new int[] {2, 3});

      assertTrue(net.checkPlacesInvariant(new int[] {1, -1}, -1)); // 2 - 3 = -1
    }

    @Test
    @DisplayName("checkPlacesInvariant with zero vector is true only for expected constant zero")
    void checkPlacesInvariant_zeroVectorBehavior() {
      PetriNet net = createSimpleNet(new int[] {5, 9});

      assertTrue(net.checkPlacesInvariant(new int[] {0, 0}, 0));
      assertFalse(net.checkPlacesInvariant(new int[] {0, 0}, 1));
    }

    @Test
    @DisplayName("checkPlacesInvariant throws IllegalArgumentException when invariant is shorter")
    void checkPlacesInvariant_throwsIllegalArgumentExceptionWhenInvariantIsShorter() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> net.checkPlacesInvariant(new int[] {1}, 1));

      assertTrue(ex.getMessage().contains("Invariant length"));
    }

    @Test
    @DisplayName("checkPlacesInvariant throws IllegalArgumentException when invariant is longer")
    void checkPlacesInvariant_throwsIllegalArgumentExceptionWhenInvariantIsLonger() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      assertThrows(
          IllegalArgumentException.class, () -> net.checkPlacesInvariant(new int[] {1, 1, 1}, 1));
    }

    @Test
    @DisplayName("checkPlacesInvariant throws NullPointerException when invariant is null")
    void checkPlacesInvariant_throwsNullPointerExceptionWhenInvariantIsNull() {
      PetriNet net = createSimpleNet(new int[] {1, 0});

      assertThrows(NullPointerException.class, () -> net.checkPlacesInvariant(null, 1));
    }

    @Test
    @DisplayName("checkPlacesInvariant can remain true before and after firing for conserved sum")
    void checkPlacesInvariant_remainsTrueForConservedInvariantAcrossFire() {
      PetriNet net = createSimpleNet(new int[] {1, 0});
      int[] invariant = {1, 1};

      assertTrue(net.checkPlacesInvariant(invariant, 1));

      net.fire(0);

      assertTrue(net.checkPlacesInvariant(invariant, 1));

      net.fire(1);

      assertTrue(net.checkPlacesInvariant(invariant, 1));
    }
  }

  @Nested
  @DisplayName("Producer-Consumer factory model")
  class ProducerConsumerModelTests {

    @Test
    @DisplayName("Factory creates net with expected initial marking")
    void createProducerConsumer_hasExpectedInitialMarking() {
      PetriNet net = PetriNet.createProducerConsumer();
      assertArrayEquals(new int[] {2, 0, 0, 2, 0, 0, 3, 1, 0}, net.getMarking());
    }

    @Test
    @DisplayName("Initial enabled transitions match expected set")
    void createProducerConsumer_initialEnabledTransitions() {
      PetriNet net = PetriNet.createProducerConsumer();

      // Only transition 0 (produce) should be enabled at the initial marking M0.
      assertArrayEquals(new int[] {0}, net.getEnabledTransitions());
    }

    @Test
    @DisplayName("Manual fire sequence follows expected reachability path and returns to M0")
    void producerConsumer_manualSequence_matchesExpectedReachability() {
      PetriNet net = PetriNet.createProducerConsumer();

      assertArrayEquals(new int[] {2, 0, 0, 2, 0, 0, 3, 1, 0}, net.getMarking());
      assertArrayEquals(new int[] {0}, net.getEnabledTransitions());

      net.fire(0);
      assertArrayEquals(new int[] {1, 1, 0, 2, 0, 0, 2, 0, 0}, net.getMarking());
      assertArrayEquals(new int[] {1}, net.getEnabledTransitions());

      net.fire(1);
      assertArrayEquals(new int[] {1, 0, 1, 2, 0, 0, 2, 1, 1}, net.getMarking());
      assertArrayEquals(new int[] {0, 2, 3}, net.getEnabledTransitions());

      net.fire(3);
      assertArrayEquals(new int[] {1, 0, 1, 1, 1, 0, 2, 0, 0}, net.getMarking());
      assertArrayEquals(new int[] {2, 4}, net.getEnabledTransitions());

      net.fire(4);
      assertArrayEquals(new int[] {1, 0, 1, 1, 0, 1, 3, 1, 0}, net.getMarking());
      assertArrayEquals(new int[] {0, 2, 5}, net.getEnabledTransitions());

      net.fire(5);
      assertArrayEquals(new int[] {1, 0, 1, 2, 0, 0, 3, 1, 0}, net.getMarking());
      assertArrayEquals(new int[] {0, 2}, net.getEnabledTransitions());

      net.fire(2);
      assertArrayEquals(new int[] {2, 0, 0, 2, 0, 0, 3, 1, 0}, net.getMarking());
      assertArrayEquals(new int[] {0}, net.getEnabledTransitions());
    }

    @Test
    @DisplayName("Disabled transition throws in producer-consumer model")
    void producerConsumer_fireDisabledTransition_throws() {
      PetriNet net = PetriNet.createProducerConsumer();

      assertThrows(IllegalStateException.class, () -> net.fire(3));
      assertArrayEquals(new int[] {2, 0, 0, 2, 0, 0, 3, 1, 0}, net.getMarking());
    }

    @Test
    @DisplayName("Producer and consumer place invariants are preserved during sequence")
    void producerConsumer_invariants_preservedAcrossSequence() {
      PetriNet net = PetriNet.createProducerConsumer();

      // producer cycle: p0 + p1 + p2 = 2
      int[] producerInvariant = {1, 1, 1, 0, 0, 0, 0, 0, 0};
      assertTrue(net.checkPlacesInvariant(producerInvariant, 2));

      // consumer cycle: p3 + p4 + p5 = 2
      int[] consumerInvariant = {0, 0, 0, 1, 1, 1, 0, 0, 0};
      assertTrue(net.checkPlacesInvariant(consumerInvariant, 2));

      // mutex invariant: p1 + p4 + p7 = 1
      int[] mutexInvariant = {0, 1, 0, 0, 1, 0, 0, 1, 0};
      assertTrue(net.checkPlacesInvariant(mutexInvariant, 1));

      // buffer invariant: p1 + p4 + p6 + p8 = 3
      int[] bufferInvariant = {0, 1, 0, 0, 1, 0, 1, 0, 1};
      assertTrue(net.checkPlacesInvariant(bufferInvariant, 3));

      int[] seq = {0, 1, 3, 4, 5, 2};
      for (int t : seq) {
        net.fire(t);
        assertTrue(net.checkPlacesInvariant(producerInvariant, 2));
        assertTrue(net.checkPlacesInvariant(consumerInvariant, 2));
        assertTrue(net.checkPlacesInvariant(mutexInvariant, 1));
        assertTrue(net.checkPlacesInvariant(bufferInvariant, 3));
      }
    }
  }
}
