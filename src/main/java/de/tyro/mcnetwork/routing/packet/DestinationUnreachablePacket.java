package de.tyro.mcnetwork.routing.packet;

import de.tyro.mcnetwork.routing.IP;

public class DestinationUnreachablePacket extends NetworkPacket implements IApplicationPaket {

    public DestinationUnreachablePacket(IP destIp) {
        super(null, destIp);
    }

    @Override
    public INetworkPacket copy() {
        throw new UnsupportedOperationException();
    }
}
