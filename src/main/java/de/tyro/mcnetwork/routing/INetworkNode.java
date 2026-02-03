package de.tyro.mcnetwork.routing;

import de.tyro.mcnetwork.routing.packet.NetworkPacket;
import de.tyro.mcnetwork.routing.protocol.RoutingProtocol;

public interface INetworkNode {

    IP getIP();

    double getX();
    double getY();
    double getZ();

    RoutingProtocol getRoutingProtocol();

    void onApplicationPacketReceived(NetworkPacket packet);

    ApplicationMessageBus getApplicationBus();
}
