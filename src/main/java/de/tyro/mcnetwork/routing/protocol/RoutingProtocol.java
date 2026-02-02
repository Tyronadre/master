package de.tyro.mcnetwork.routing.protocol;

import de.tyro.mcnetwork.routing.node.SimNode;
import de.tyro.mcnetwork.routing.packet.Packet;
import de.tyro.mcnetwork.routing.routing.RoutingTable;

import java.util.UUID;

public interface RoutingProtocol {

    void onReceive(Packet packet, SimNode self, SimNode sender);

    void onTick(SimNode tvSimNode);

    RoutingTable getRoutingTable();

    ProtocolType getType();

    void discoverRoute(SimNode self, UUID destination);
}
