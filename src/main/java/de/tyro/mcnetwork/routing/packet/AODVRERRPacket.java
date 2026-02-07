package de.tyro.mcnetwork.routing.packet;

import de.tyro.mcnetwork.routing.IP;

import java.util.HashMap;
import java.util.Map;

public class AODVRERRPacket extends NetworkPacket implements IProtocolPaket {
    public final boolean noDelete;
    public final Map<IP, Integer> unreachable = new HashMap<>();

    protected AODVRERRPacket(IP sourceIp, IP destinationIp, boolean noDelete) {
        super(sourceIp, destinationIp);
        this.noDelete = noDelete;
    }

    public void addUnreachable(IP ip, int seqNumber) {
        unreachable.put(ip, seqNumber);
    }

    @Override
    public INetworkPacket copy() {
        return null;
    }
}
