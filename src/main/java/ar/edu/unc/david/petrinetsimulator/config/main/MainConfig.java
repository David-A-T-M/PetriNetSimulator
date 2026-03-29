package ar.edu.unc.david.petrinetsimulator.config.main;

import java.util.List;

/**
 * Represents the main configuration for the Petri net simulation, including the active network and
 * available networks.
 */
public record MainConfig(String activeNetworkId, List<NetworkEntry> networks) {}
