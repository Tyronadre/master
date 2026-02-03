package de.tyro.mcnetwork.routing.packet;

import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;

public class AODVRREPPacket extends NetworkPacket {

    public final boolean repairFlag;
    public final boolean ackRequired;
    public int hopCount;
    public final int destSeqNumber;
    public final long lifetime;


    public AODVRREPPacket(IP source, IP dest, IP prev, boolean repairFlag, boolean ackRequired, int hopCount, int destSeqNumber, long lifetime) {
        super(source, dest, prev);
        this.repairFlag = repairFlag;
        this.ackRequired = ackRequired;
        this.hopCount = hopCount;
        this.destSeqNumber = destSeqNumber;
        this.lifetime = lifetime;
    }

    public NetworkPacket hop(INetworkNode self) {
        return new AODVRREPPacket(sourceIp, destinationIp, self.getIP(), repairFlag, ackRequired, hopCount++, destSeqNumber, lifetime);
    }
}
