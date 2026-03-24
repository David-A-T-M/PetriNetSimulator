package ar.edu.unc.david.petrinetsimulator;

import java.util.Objects;
import java.util.Random;

/** A simple policy that randomly selects one of the candidate transitions to fire. */
public class RandomPolicy implements Policy {
  private final Random random;

  public RandomPolicy() {
    this(new Random());
  }

  RandomPolicy(Random random) {
    this.random = Objects.requireNonNull(random);
  }

  @Override
  public int decide(int[] candidates) {
    if (candidates == null) {
      throw new NullPointerException("Candidates array cannot be null.");
    }
    if (candidates.length == 0) {
      throw new IllegalArgumentException("No candidates to choose from.");
    }
    return candidates[random.nextInt(candidates.length)];
  }
}
