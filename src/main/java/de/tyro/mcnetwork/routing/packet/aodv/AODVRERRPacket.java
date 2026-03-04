package de.tyro.mcnetwork.routing.packet.aodv;

import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;

import java.util.HashMap;
import java.util.Map;

public class AODVRERRPacket extends NetworkPacket implements IProtocolPaket {
    public final boolean noDelete;
    public final Map<IP, Integer> unreachable = new HashMap<>();

    public AODVRERRPacket(IP sourceIp, IP destinationIp, boolean noDelete) {
        super(sourceIp, destinationIp);
        this.noDelete = noDelete;
    }

    public void addUnreachable(IP ip, int seqNumber) {
        unreachable.put(ip, seqNumber);
    }

    public int destCount() {
        return unreachable.size();
    }

    @Override
    public INetworkPacket copy() {
        return null;
    }

}
