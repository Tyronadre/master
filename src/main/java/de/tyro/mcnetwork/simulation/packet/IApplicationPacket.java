package de.tyro.mcnetwork.simulation.packet;

import de.tyro.mcnetwork.simulation.SimulationEngine;

public interface IApplicationPacket extends INetworkPacket {
    default SimulationEngine getSimulationEngine() {
        if (getNetworkFrame() == null) return SimulationEngine.getInstance(true);
        return SimulationEngine.getInstance(getNetworkFrame().getFrom().getLevel().isClientSide());
    }
}
