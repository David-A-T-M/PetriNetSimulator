package ar.edu.unc.david.petrinetsimulator.policy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.unc.david.petrinetsimulator.config.logic.PolicyGroupConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for the ProportionPolicy class. */
class ProportionPolicyTest {

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {
    @Test
    @DisplayName("rejects null groups")
    void rejectsNullGroups() {
      assertThrows(NullPointerException.class, () -> new ProportionPolicy(null));
    }

    @Test
    @DisplayName("rejects empty groups")
    void rejectsEmptyGroups() {
      assertThrows(IllegalArgumentException.class, () -> new ProportionPolicy(List.of()));
    }
  }

  @Nested
  @DisplayName("decide")
  class DecideTests {
    @Test
    @DisplayName("balances candidates according to equal weights")
    void decidesUsingEqualWeights() {
      ProportionPolicy policy =
          new ProportionPolicy(List.of(new PolicyGroupConfig(new int[] {2, 3}, new int[] {1, 1})));

      int[] observed = new int[6];
      for (int i = 0; i < observed.length; i++) {
        observed[i] = policy.decide(new int[] {2, 3});
      }

      assertArrayEquals(new int[] {2, 3, 2, 3, 2, 3}, observed);
    }

    @Test
    @DisplayName("follows weighted proportions for uneven weights")
    void decidesUsingWeightedProportions() {
      ProportionPolicy policy =
          new ProportionPolicy(
              List.of(new PolicyGroupConfig(new int[] {10, 20}, new int[] {1, 3})));

      int[] observed = new int[8];
      for (int i = 0; i < observed.length; i++) {
        observed[i] = policy.decide(new int[] {10, 20});
      }

      assertArrayEquals(new int[] {10, 20, 20, 20, 10, 20, 20, 20}, observed);
    }

    @Test
    @DisplayName("throws when candidate is null")
    void throwsWhenCandidateIsNull() {
      ProportionPolicy policy =
          new ProportionPolicy(List.of(new PolicyGroupConfig(new int[] {1}, new int[] {1})));
      assertThrows(NullPointerException.class, () -> policy.decide(null));
    }

    @Test
    @DisplayName("throws when candidate is empty")
    void throwsWhenCandidateIsEmpty() {
      ProportionPolicy policy =
          new ProportionPolicy(List.of(new PolicyGroupConfig(new int[] {1}, new int[] {1})));
      assertThrows(IllegalArgumentException.class, () -> policy.decide(new int[0]));
    }

    @Test
    @DisplayName("falls back deterministically when a candidate is outside any configured group")
    void fallsBackWhenCandidateIsOutsideConfiguredGroups() {
      ProportionPolicy policy =
          new ProportionPolicy(List.of(new PolicyGroupConfig(new int[] {2, 3}, new int[] {1, 1})));

      assertEquals(99, policy.decide(new int[] {99, 2, 3}));
    }
  }
}
