package de.tyro.mcnetwork.simulation.protocol;


import de.tyro.mcnetwork.simulation.IHudRenderer;
import de.tyro.mcnetwork.simulation.INetworkNode;
import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.packet.INetworkPacket;
import de.tyro.mcnetwork.simulation.packet.IProtocolPaket;

public interface IRoutingProtocol extends IHudRenderer {

    void onProtocolPacketReceived(IProtocolPaket packet);

    void send(INetworkPacket packet, int ttl);

    void discoverRoute(IP destinationIp);

    void simTick();

    boolean hasRoute(IP destination);

    INetworkNode getNode();

    ProtocolSettings getSettings();
}