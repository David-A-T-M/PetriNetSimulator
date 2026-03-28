package ar.edu.unc.david.petrinetsimulator.config;

import java.util.List;

/** Configuration for the layout of the Petri net visualization. */
public record LayoutConfig(List<PlaceLayout> places, List<TransitionLayout> transitions) {}
