package ar.edu.unc.david.petrinetsimulator.config.main;

/**
 * Represents an entry for a Petri net network configuration, including its ID, name, and paths to
 * logic and layout files.
 */
public record NetworkEntry(String id, String name, String logicPath, String layoutPath) {}
