package de.tyro.mcnetwork.routing.event;

import de.tyro.mcnetwork.routing.core.SimulationEngine;
import de.tyro.mcnetwork.routing.node.SimNode;
import de.tyro.mcnetwork.routing.packet.Packet;

public class PacketForwardEvent implements SimulationEvent {

    private final SimulationEngine engine = SimulationEngine.INSTANCE;
    private final SimNode sender;
    private final Packet packet;

    public PacketForwardEvent(SimNode sender, Packet packet) {
        this.sender = sender;
        this.packet = packet;
    }

    @Override
    public void execute() {
        for (SimNode neighbor : engine.getNeighbors(sender)) {
            engine.enqueue(
                    new PacketReceiveEvent(sender, neighbor, packet.copy())
            );
        }
    }

    @Override
    public String describe() {
        return "Node " + sender.getId() + " forwards " + packet.getType();
    }
}

