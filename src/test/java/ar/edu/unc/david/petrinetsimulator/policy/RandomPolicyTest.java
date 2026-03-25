package ar.edu.unc.david.petrinetsimulator.policy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for the RandomPolicy class. */
public class RandomPolicyTest {

  @Nested
  @DisplayName("Constructors")
  class ConstructorTests {

    @Test
    @DisplayName("Default constructor creates policy without throwing")
    void constructor_default_doesNotThrow() {
      assertDoesNotThrow(() -> new RandomPolicy());
    }

    @Test
    @DisplayName("Injected constructor throws NullPointerException when random is null")
    void constructor_injected_throwsWhenRandomIsNull() {
      assertThrows(NullPointerException.class, () -> new RandomPolicy(null));
    }

    @Test
    @DisplayName("Injected constructor accepts valid Random instance")
    void constructor_injected_acceptsValidRandom() {
      Random customRandom = new Random(12345);
      assertDoesNotThrow(() -> new RandomPolicy(customRandom));
    }
  }

  @Nested
  @DisplayName("decide")
  class DecideTests {

    @Test
    @DisplayName("Throws IllegalArgumentException when candidates array is empty")
    void decide_throwsWhenCandidatesIsEmpty() {
      RandomPolicy policy = new RandomPolicy();

      assertThrows(IllegalArgumentException.class, () -> policy.decide(new int[0]));
    }

    @Test
    @DisplayName("Throws NullPointerException when candidates array is null")
    void decide_throwsWhenCandidatesIsNull() {
      RandomPolicy policy = new RandomPolicy();

      assertThrows(NullPointerException.class, () -> policy.decide(null));
    }

    @Test
    @DisplayName("With one candidate always returns that candidate")
    void decide_withOneCandidate_alwaysReturnsSameValue() {
      RandomPolicy policy = new RandomPolicy();

      for (int i = 0; i < 20; i++) {
        assertEquals(42, policy.decide(new int[] {42}));
      }
    }

    @Test
    @DisplayName("Uses random index to select the expected candidate")
    void decide_usesRandomIndexToSelectCandidate() {
      StubRandom stubRandom = new StubRandom(2, 0, 1);
      RandomPolicy policy = new RandomPolicy(stubRandom);
      int[] candidates = {10, 20, 30};

      assertEquals(30, policy.decide(candidates));
      assertEquals(10, policy.decide(candidates));
      assertEquals(20, policy.decide(candidates));
    }

    @Test
    @DisplayName("Passes candidates length as bound to Random.nextInt")
    void decide_passesCandidatesLengthAsBound() {
      StubRandom stubRandom = new StubRandom(0);
      RandomPolicy policy = new RandomPolicy(stubRandom);

      policy.decide(new int[] {7, 8, 9, 10});

      assertEquals(4, stubRandom.getLastBound());
    }

    @Test
    @DisplayName("Supports negative and duplicated candidate values")
    void decide_supportsNegativeAndDuplicatedValues() {
      StubRandom stubRandom = new StubRandom(1, 2, 0);
      RandomPolicy policy = new RandomPolicy(stubRandom);
      int[] candidates = {-5, -5, 42};

      assertEquals(-5, policy.decide(candidates));
      assertEquals(42, policy.decide(candidates));
      assertEquals(-5, policy.decide(candidates));
    }

    @Test
    @DisplayName("Does not mutate candidates array")
    void decide_doesNotMutateCandidatesArray() {
      StubRandom stubRandom = new StubRandom(1);
      RandomPolicy policy = new RandomPolicy(stubRandom);
      int[] candidates = {3, 6, 9};
      int[] before = candidates.clone();

      policy.decide(candidates);

      assertArrayEquals(before, candidates);
    }

    @Test
    @DisplayName("Returned value always belongs to candidates")
    void decide_returnedValueAlwaysBelongsToCandidates() {
      RandomPolicy policy = new RandomPolicy();
      int[] candidates = {4, 8, 15, 16, 23, 42};

      for (int i = 0; i < 200; i++) {
        int chosen = policy.decide(candidates);
        assertTrue(contains(candidates, chosen));
      }
    }
  }

  private static boolean contains(int[] array, int value) {
    for (int v : array) {
      if (v == value) {
        return true;
      }
    }
    return false;
  }

  /** Deterministic Random for tests: returns a predefined sequence of indexes. */
  private static final class StubRandom extends Random {
    private final int[] sequence;
    private int cursor;
    private int lastBound = -1;

    StubRandom(int... sequence) {
      if (sequence == null || sequence.length == 0) {
        throw new IllegalArgumentException("Sequence must not be null or empty.");
      }
      this.sequence = sequence.clone();
    }

    @Override
    public int nextInt(int bound) {
      lastBound = bound;
      int value = sequence[cursor % sequence.length];
      cursor++;

      if (value < 0 || value >= bound) {
        throw new IllegalStateException("StubRandom value " + value + " is out of bound " + bound);
      }
      return value;
    }

    int getLastBound() {
      return lastBound;
    }
  }
}
