package de.tyro.mcnetwork.simulation.packet.application;

import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.packet.IApplicationPacket;
import de.tyro.mcnetwork.simulation.packet.INetworkPacket;
import de.tyro.mcnetwork.simulation.packet.NetworkPacket;

public class DestinationUnreachablePacket extends NetworkPacket implements IApplicationPacket {

    public DestinationUnreachablePacket(IP thisIP, IP destIp) {
        super(thisIP, destIp);
    }

    @Override
    public INetworkPacket copy() {
        throw new UnsupportedOperationException();
    }
}
