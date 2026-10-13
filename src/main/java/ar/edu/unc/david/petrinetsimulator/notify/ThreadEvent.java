package ar.edu.unc.david.petrinetsimulator.notify;

/** Represents an event related to thread activity in the Petri net simulation. */
public record ThreadEvent(int transitionId, String threadId) implements PetriEvent {}
