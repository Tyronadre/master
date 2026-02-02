package de.tyro.mcnetwork.routing.node;

import de.tyro.mcnetwork.routing.core.SimulationEngine;
import de.tyro.mcnetwork.routing.core.SimulationRegistry;
import de.tyro.mcnetwork.routing.event.SimulationEvent;
import de.tyro.mcnetwork.routing.packet.Packet;
import de.tyro.mcnetwork.routing.protocol.RoutingProtocol;
import net.minecraft.core.BlockPos;

import java.util.UUID;

public class SimNode {
    private final UUID id;
    private BlockPos pos;

    private final NodeData nodeData;
    private final RoutingProtocol protocol;

    public SimNode(UUID id, NodeData nodeData, BlockPos pos, RoutingProtocol protocol) {
        this.id = id;
        this.nodeData = nodeData;
        this.protocol = protocol;
    }

    public void receive(Packet packet) {
        protocol.onReceive(packet, this, SimulationEngine.INSTANCE.getNodeById(packet.getSource()));
    }

    public void tick() {
        protocol.onTick(this);
    }

    public UUID getId() {
        return id;
    }

    public BlockPos getPos() {
        return pos;
    }

    public NodeData getNodeData() {
        return nodeData;
    }

    public RoutingProtocol getProtocol() {
        return protocol;
    }

    /**
     * Sendet ein Paket an das gegebene Ziel. Nutzt die Routingtabelle, um den nächsten Hop zu bestimmen.
     * Falls kein Eintrag existiert, wird eine Route Discovery angestoßen.
     */
    public void send(Packet packet, UUID destination) {
        var routingTable = protocol.getRoutingTable();
        var entry = routingTable.getEntry(destination);
        if (entry != null && entry.nextHop() != null) {
            // Weiterleitung an den nächsten Hop
            SimNode nextNode = SimulationEngine.INSTANCE.getNodeById(entry.nextHop());
            if (nextNode != null) {
                nextNode.receive(packet);
            } else {
                // Fehlerbehandlung: NextHop existiert nicht
                System.err.println("[SimNode] NextHop nicht gefunden: " + entry.nextHop());
            }
        } else {
            // Kein Routing-Eintrag: Route Discovery anstoßen
            System.out.println("[SimNode] Keine Route zu " + destination + ", starte Route Discovery.");
            protocol.discoverRoute(this, destination);
            // Optional: Paket zwischenspeichern, bis Route gefunden
        }
    }
}
