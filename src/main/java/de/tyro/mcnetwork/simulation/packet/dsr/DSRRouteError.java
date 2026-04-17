package de.tyro.mcnetwork.simulation.packet.dsr;

import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.packet.INetworkPacket;
import de.tyro.mcnetwork.simulation.packet.IProtocolPaket;
import de.tyro.mcnetwork.simulation.packet.NetworkPacket;

import java.util.UUID;

public class DSRRouteError extends NetworkPacket implements IProtocolPaket {
    public IP errorSourceAddress;
    public IP errorDestinationAddress;
    public IP unreachableNodeAddress;


    public DSRRouteError(IP originatorIP, IP destinationIP) {
        super(originatorIP, destinationIP);
    }

    public DSRRouteError(UUID id, IP originatorIP, IP destinationIP, IP errorSourceAddress, IP errorDestinationAddress, IP unreachableNodeAddress) {
        super(id, originatorIP, destinationIP);
        this.errorSourceAddress = errorSourceAddress;
        this.errorDestinationAddress = errorDestinationAddress;
        this.unreachableNodeAddress = unreachableNodeAddress;
    }

    @Override
    public INetworkPacket copy() {
        return new DSRRouteError(this.getOriginatorIP(), this.getDestinationIP());
    }


}
