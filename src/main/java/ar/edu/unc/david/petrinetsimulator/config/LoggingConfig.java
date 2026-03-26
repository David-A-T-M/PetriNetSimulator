package ar.edu.unc.david.petrinetsimulator.config;

/** Configuration for logging in a Petri net simulation. */
public record LoggingConfig(String file, boolean logWaitWake) {}
