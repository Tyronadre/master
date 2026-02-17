package de.tyro.mcnetwork.routing.packet;

import de.tyro.mcnetwork.routing.SimulationEngine;

public interface IApplicationPaket extends INetworkPacket {
    default SimulationEngine getSimulationEngine() {
        return SimulationEngine.getInstance(getNetworkFrame().getFrom().getLevel().isClientSide());
    }
}
