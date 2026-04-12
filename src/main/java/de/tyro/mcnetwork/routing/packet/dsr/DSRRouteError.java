package de.tyro.mcnetwork.routing.packet.dsr;

import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;

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
