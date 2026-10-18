package ar.edu.unc.david.petrinetsimulator.config.layout;

import java.util.List;

public record ThreadPathConfig(String arc, List<WaypointConfig> waypoints) implements PathConfig {}
