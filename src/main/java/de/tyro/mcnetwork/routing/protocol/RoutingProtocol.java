package de.tyro.mcnetwork.routing.protocol;


import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;

import java.util.Collection;

public interface RoutingProtocol {

    void onProtocolPacketReceived(IProtocolPaket packet);

    void send(INetworkPacket packet, int ttl);

    void discoverRoute(IP destinationIp);

    void tick();

    boolean hasRoute(IP destination);

    Collection<String> renderData();

}