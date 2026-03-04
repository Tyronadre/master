package de.tyro.mcnetwork.routing.packet.application;

import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.IApplicationPacket;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;

public class DestinationUnreachablePacket extends NetworkPacket implements IApplicationPacket {

    public DestinationUnreachablePacket(IP thisIP, IP destIp) {
        super(thisIP, destIp);
    }

    @Override
    public INetworkPacket copy() {
        throw new UnsupportedOperationException();
    }
}
