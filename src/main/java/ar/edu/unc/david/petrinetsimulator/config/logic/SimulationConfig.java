package ar.edu.unc.david.petrinetsimulator.config.logic;

import java.util.List;

/** Configuration for a Petri net simulation. */
public record SimulationConfig(
    NetConfig net,
    List<PlaceInvariantConfig> placeInvariants,
    List<AgentConfig> agents,
    LoggingConfig logging,
    RuntimeConfig runtime,
    PolicyConfig policy) {}
