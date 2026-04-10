package de.tyro.mcnetwork.routing.packet.aodv;

import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AODVRERRPacket extends NetworkPacket implements IProtocolPaket {
    public final boolean noDelete;
    public final Map<IP, Integer> unreachable;

    public AODVRERRPacket(IP sourceIp, IP destinationIp, boolean noDelete) {
        super(sourceIp, destinationIp);
        this.noDelete = noDelete;
        this.unreachable = new HashMap<>();
    }

    public AODVRERRPacket(UUID id, IP sourceIp, IP destinationIp, boolean noDelete, Map<IP, Integer> unreachable) {
        super(id, sourceIp, destinationIp);
        this.noDelete = noDelete;
        this.unreachable = unreachable;
    }

    public void addUnreachable(IP ip, int seqNumber) {
        unreachable.put(ip, seqNumber);
    }

    public int destCount() {
        return unreachable.size();
    }

    @Override
    public INetworkPacket copy() {
        return new AODVRERRPacket(id, getOriginatorIP(), getDestinationIP(), noDelete, unreachable);
    }

}
