package ar.edu.unc.david.petrinetsimulator;

/** Interface for monitoring the firing of transitions in a Petri net. */
public interface MonitorInterface {

  boolean fireTransition(int transition);
}
