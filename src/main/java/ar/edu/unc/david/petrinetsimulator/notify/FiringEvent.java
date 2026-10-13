package ar.edu.unc.david.petrinetsimulator.notify;

/** Represents an event in the Petri net simulation, specifically the firing of a transition. */
public record FiringEvent(
    int transitionId, String threadId, int[] markingBefore, int[] newMarking, long timestamp)
    implements PetriEvent {}
