package de.tyro.mcnetwork.routing.protocol;

import de.tyro.mcnetwork.routing.core.SimulationEngine;
import de.tyro.mcnetwork.routing.event.PacketForwardEvent;
import de.tyro.mcnetwork.routing.event.PacketReceiveEvent;
import de.tyro.mcnetwork.routing.node.SimNode;
import de.tyro.mcnetwork.routing.packet.AodvRrepPacket;
import de.tyro.mcnetwork.routing.packet.AodvRreqPacket;
import de.tyro.mcnetwork.routing.packet.Packet;
import de.tyro.mcnetwork.routing.routing.AodvRoutingEntry;
import de.tyro.mcnetwork.routing.routing.AodvRoutingTable;
import de.tyro.mcnetwork.routing.routing.RoutingEntry;
import de.tyro.mcnetwork.routing.routing.RoutingTable;

import java.util.HashSet;
import java.util.Set;

public class AodvProtocol implements RoutingProtocol {

    private final SimulationEngine engine = SimulationEngine.INSTANCE;
    private final AodvRoutingTable routingTable = new AodvRoutingTable();
    private int sequenceNumber = 0;
    private int requestIdCounter = 0;

    private final Set<String> seenRreqs = new HashSet<>();

    @Override
    public ProtocolType getType() {
        return ProtocolType.AODV;
    }

    @Override
    public void onReceive(Packet packet, SimNode node, SimNode sender) {

        if (packet instanceof AodvRreqPacket rreq) {
            handleRreq(rreq, node, sender);
        }

        if (packet instanceof AodvRrepPacket rrep) {
            handleRrep(rrep, node, sender);
        }
    }

    @Override
    public void onTick(SimNode node) {
        // AODV ist rein reaktiv → kein periodischer Tick nötig
    }

    @Override
    public RoutingTable<AodvRoutingEntry> getRoutingTable() {
        return routingTable;
    }

    private void handleRreq(AodvRreqPacket rreq, SimNode self, SimNode sender) {

        String key = rreq.getSource() + ":" + rreq.getRequestId();
        if (!seenRreqs.add(key)) return;

        // Reverse Route
        routingTable.update(new AodvRoutingEntry(rreq.getSource(), sender.getId(), rreq.getHopCount(), rreq.getSourceSequenceNumber(), System.currentTimeMillis() + 30_000));

        if (self.getId().equals(rreq.getDestination())) {
            AodvRrepPacket rrep = new AodvRrepPacket(rreq.getDestination(), rreq.getSource(), sequenceNumber++, Integer.MAX_VALUE);

            engine.enqueue(new PacketForwardEvent(self, rrep));
            return;
        }

        rreq.incrementHopCount();
        engine.enqueue(new PacketForwardEvent(self, rreq));
    }


    private void handleRrep(AodvRrepPacket rrep, SimNode self, SimNode sender) {

        // 1. Forward Route zum Ziel (destination des ursprünglichen RREQ)
        routingTable.update(new AodvRoutingEntry(rrep.getSource(),          // Zielnode
                sender.getId(),            // NextHop Richtung Ziel
                rrep.getHopCount(), rrep.getDestinationSequenceNumber(), System.currentTimeMillis() + 30_000));

        // 2. Bin ich der Ursprung der RREQ?
        if (self.getId().equals(rrep.getDestination())) {
            // Route Discovery abgeschlossen
            return;
        }

        // 3. Reverse Route zum Ursprung der RREQ nachschlagen
        RoutingEntry reverseRoute = routingTable.getEntry(rrep.getDestination());

        // sollte nicht passieren, aber didaktisch sinnvoll
        if (reverseRoute == null) return;


        // 4. RREP gezielt weiterleiten
        rrep.incrementHopCount();

        SimNode nextHop = engine.getNodeById(reverseRoute.nextHop());

        engine.enqueue(new PacketReceiveEvent(self, nextHop, rrep));
    }


}


