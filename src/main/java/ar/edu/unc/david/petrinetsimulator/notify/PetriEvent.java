package ar.edu.unc.david.petrinetsimulator.notify;

/** A sealed interface representing events in the Petri net simulation. */
public sealed interface PetriEvent permits FiringEvent, ThreadEvent {
  int transitionId();
}
