package de.tyro.mcnetwork.simulation.packet.lar;

import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.packet.INetworkPacket;
import de.tyro.mcnetwork.simulation.packet.IProtocolPaket;
import de.tyro.mcnetwork.simulation.packet.NetworkPacket;

import java.util.UUID;

public class LARRouteError extends NetworkPacket implements IProtocolPaket {
    public final IP unreachableDestination;

    public LARRouteError(IP sourceIp, IP destinationIp, IP unreachableDestination) {
        super(sourceIp, destinationIp);
        this.unreachableDestination = unreachableDestination;
    }

    public LARRouteError(UUID uuid, IP sourceIp, IP destinationIp, IP unreachableDestination) {
        super(uuid, sourceIp, destinationIp);
        this.unreachableDestination = unreachableDestination;
    }

    public INetworkPacket copy() {
        return new LARRouteError(getOriginatorIP(), getDestinationIP(), unreachableDestination);
    }
}
