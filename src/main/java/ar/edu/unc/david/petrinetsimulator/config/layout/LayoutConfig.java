package ar.edu.unc.david.petrinetsimulator.config.layout;

import java.util.List;

/** Configuration for the layout of the Petri net visualization. */
public record LayoutConfig(
    List<PlaceLayout> places,
    double transW,
    double transH,
    List<TransitionLayout> transitions,
    List<ArcLayoutConfig> arcs) {}
