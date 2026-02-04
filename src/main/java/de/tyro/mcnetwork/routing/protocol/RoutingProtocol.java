package de.tyro.mcnetwork.routing.protocol;


import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;

import java.util.Collection;

public interface RoutingProtocol {

    void onProtocolPacketReceived(INetworkNode self, IProtocolPaket packet);

    void sendData(INetworkNode self, INetworkPacket packet);

    void onSendRequest(INetworkNode self, IP destinationIp);

    void tick(INetworkNode self);

    boolean hasRoute(IP destination);

    Collection<String> renderData();
}