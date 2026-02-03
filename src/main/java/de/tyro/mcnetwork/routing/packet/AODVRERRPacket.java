package de.tyro.mcnetwork.routing.packet;

import de.tyro.mcnetwork.routing.IP;

import java.util.HashMap;
import java.util.Map;

public class AODVRERRPacket extends NetworkPacket{
    public final boolean noDelete;
    public final Map<IP, Integer> unreachable = new HashMap<IP, Integer>();

    protected AODVRERRPacket(IP sourceIp, IP destinationIp, IP previousHopIp, boolean noDelete) {
        super(sourceIp, destinationIp, previousHopIp);
        this.noDelete = noDelete;
    }

    public void addUnreachable(IP ip, int seqNumber) {
        unreachable.put(ip, seqNumber);
    }
}
