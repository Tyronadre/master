package de.tyro.mcnetwork.routing.event;

import de.tyro.mcnetwork.routing.node.SimNode;
import de.tyro.mcnetwork.routing.packet.EmptyPacket;
import de.tyro.mcnetwork.routing.packet.Packet;
import de.tyro.mcnetwork.routing.core.SimulationEngine;
import java.util.UUID;

public class TestPacketEvent implements SimulationEvent {
    private final SimNode source;
    private final UUID destination;
    private int delayTicks;

    public TestPacketEvent(SimNode source, UUID destination, int delayTicks) {
        this.source = source;
        this.destination = destination;
        this.delayTicks = delayTicks;
    }

    @Override
    public void execute() {
        if (delayTicks > 0) {
            delayTicks--;
            // Re-enqueue until delay abgelaufen
            SimulationEngine.INSTANCE.enqueue(this);
            return;
        }
        // Sende ein TestPacket
        Packet packet = new EmptyPacket(source.getId(), destination, 100);
        source.send(packet, destination);
    }

    @Override
    public String describe() {
        return "";
    }
}

