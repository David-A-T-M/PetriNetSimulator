package ar.edu.unc.david.petrinetsimulator.config.layout;

import java.util.List;

/** Represents the layout configuration for an arc in the Petri net visualization. */
public record ArcLayoutConfig(String arc, List<WaypointConfig> waypoints) {}
