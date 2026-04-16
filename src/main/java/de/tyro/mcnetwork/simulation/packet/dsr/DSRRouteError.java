package de.tyro.mcnetwork.simulation.packet.dsr;

import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.packet.INetworkPacket;
import de.tyro.mcnetwork.simulation.packet.IProtocolPaket;
import de.tyro.mcnetwork.simulation.packet.NetworkPacket;

public class DSRRouteError extends NetworkPacket implements IProtocolPaket {
    public IP errorSourceAddress;
    public IP errorDestinationAddress;
    public IP unreachableNodeAddress;


    public DSRRouteError(IP originatorIP, IP destinationIP) {
        super(originatorIP, destinationIP);
    }

    @Override
    public INetworkPacket copy() {
        return new DSRRouteError(this.getOriginatorIP(), this.getDestinationIP());
    }


}
