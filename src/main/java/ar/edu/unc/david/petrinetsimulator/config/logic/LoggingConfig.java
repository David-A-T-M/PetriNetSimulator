package ar.edu.unc.david.petrinetsimulator.config.logic;

/** Configuration for logging in a Petri net simulation. */
public record LoggingConfig(String file, boolean logWaitWake) {}
