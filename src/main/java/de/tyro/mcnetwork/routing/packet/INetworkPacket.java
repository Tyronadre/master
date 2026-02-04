package de.tyro.mcnetwork.routing.packet;

import de.tyro.mcnetwork.routing.IP;

import java.util.UUID;

public interface INetworkPacket extends IPacketRenderable {
    UUID getId();
    IP getDestinationIp();
    IP getSourceIp();
    IP getPreviousHopIp();
    String getPacketTypeName();
    INetworkPacket copy();
}
