package de.tyro.mcnetwork.routing.protocol;


import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;

import java.util.Collection;

public interface RoutingProtocol {

    void onPacketReceived(INetworkNode self, NetworkPacket packet);

    void sendData(INetworkNode self, NetworkPacket packet);

    void onSendRequest(INetworkNode self, IP destinationIp);

    void tick(INetworkNode self);

    boolean hasRoute(IP destination);

    Collection<String> renderData();
}