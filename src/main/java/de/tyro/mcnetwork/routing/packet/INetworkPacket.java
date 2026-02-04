package de.tyro.mcnetwork.routing.packet;

import de.tyro.mcnetwork.routing.IP;

import java.util.List;
import java.util.UUID;

public interface INetworkPacket {
    UUID getId();
    IP getDestinationIp();
    IP getSourceIp();
    IP getPreviousHopIp();
    List<String> getRenderContent();
    String getPacketTypeName();
}
