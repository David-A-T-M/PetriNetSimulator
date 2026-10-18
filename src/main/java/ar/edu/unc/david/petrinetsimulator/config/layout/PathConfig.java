package ar.edu.unc.david.petrinetsimulator.config.layout;

import java.util.List;

public interface PathConfig {
  String arc();

  List<WaypointConfig> waypoints();
}
