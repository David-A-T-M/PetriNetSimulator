package ar.edu.unc.david.petrinetsimulator.core;

/** Represents an event in the Petri net simulation, specifically the firing of a transition. */
public record PetriEvent(int transitionId, int[] markingBefore, int[] newMarking, long timestamp) {}
