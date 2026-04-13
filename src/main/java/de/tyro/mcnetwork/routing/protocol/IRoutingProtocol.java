package de.tyro.mcnetwork.routing.protocol;


import de.tyro.mcnetwork.routing.IHudRenderer;
import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;

import java.util.Map;

public interface IRoutingProtocol extends IHudRenderer {

    void onProtocolPacketReceived(IProtocolPaket packet);

    void send(INetworkPacket packet, int ttl);

    void discoverRoute(IP destinationIp);

    void simTick();

    boolean hasRoute(IP destination);

    INetworkNode getNode();

    ProtocolSettings getSettings();
}