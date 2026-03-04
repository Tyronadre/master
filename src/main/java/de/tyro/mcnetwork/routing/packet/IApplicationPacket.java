package de.tyro.mcnetwork.routing.packet;

import de.tyro.mcnetwork.routing.SimulationEngine;

public interface IApplicationPacket extends INetworkPacket {
    default SimulationEngine getSimulationEngine() {
        if (getNetworkFrame() == null) return SimulationEngine.getInstance(true);
        return SimulationEngine.getInstance(getNetworkFrame().getFrom().getLevel().isClientSide());
    }
}
