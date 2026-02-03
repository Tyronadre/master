package de.tyro.mcnetwork.routing.packet;

import de.tyro.mcnetwork.routing.IP;

import java.util.List;
import java.util.UUID;

public abstract class NetworkPacket {

    public final UUID id = UUID.randomUUID();
    public final IP sourceIp;
    public final IP destinationIp;
    public final IP previousHopIP;

    protected NetworkPacket(IP sourceIp, IP destinationIp, IP previousHopIP) {
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        this.previousHopIP = previousHopIP;
    }

    protected  NetworkPacket(IP sourceIp, IP destinationIp) {
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        previousHopIP = null;
    }

    public Iterable<String> getRenderContent(){
        return List.of();
    }

    public String getPacketTypeName() {
        return this.getClass().getSimpleName();
    }
}
