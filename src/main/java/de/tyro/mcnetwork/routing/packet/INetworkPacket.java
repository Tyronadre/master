package de.tyro.mcnetwork.routing.packet;

import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.NetworkFrame;

import java.util.UUID;

public interface INetworkPacket extends IPacketRenderable {
    UUID getId();
    IP getDestinationIP();
    IP getOriginatorIP();
    INetworkPacket copy();

    void setFrame(NetworkFrame inFlightPacket);
    NetworkFrame getNetworkFrame();
}
