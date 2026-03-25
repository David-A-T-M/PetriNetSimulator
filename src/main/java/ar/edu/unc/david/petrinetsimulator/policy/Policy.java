package ar.edu.unc.david.petrinetsimulator.policy;

/** Interface for defining policies to select which transition to fire among candidates. */
public interface Policy {
  int decide(int[] candidates);
}
