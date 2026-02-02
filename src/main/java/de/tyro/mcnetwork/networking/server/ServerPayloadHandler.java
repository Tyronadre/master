package de.tyro.mcnetwork.networking.server;

import de.tyro.mcnetwork.networking.payload.SimulationControlPacket;
import de.tyro.mcnetwork.routing.core.SimulationRegistry;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {

    public static void handleDataOnNetwork(final SimulationControlPacket simulationControlPacket, IPayloadContext context) {
        var player = context.player();
        switch (simulationControlPacket.action) {
            case START -> SimulationRegistry.startSimulation(player);
            case STOP -> SimulationRegistry.stopSimulation(player);
            case RESET -> SimulationRegistry.resetSimulation(player);
            case TICK -> SimulationRegistry.tickSimulation(player);
        }
    }
}
