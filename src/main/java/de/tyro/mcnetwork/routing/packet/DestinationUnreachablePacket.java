package de.tyro.mcnetwork.routing.packet;

import de.tyro.mcnetwork.routing.IP;

public class DestinationUnreachablePacket extends NetworkPacket implements IApplicationPaket {

    public DestinationUnreachablePacket(IP thisIP, IP destIp) {
        super(thisIP, destIp);
    }

    @Override
    public INetworkPacket copy() {
        throw new UnsupportedOperationException();
    }
}
