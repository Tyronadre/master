package de.tyro.mcnetwork.routing.packet.olsr;

import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;

import java.util.Set;
import java.util.UUID;

public class HelloPacket extends NetworkPacket implements IProtocolPaket {
    public final Set<IP> neighbors;
    public final int willingness;

    public HelloPacket(IP sourceIp, Set<IP> neighbors, int willingness) {
        super(sourceIp, IP.BROADCAST);
        this.neighbors = neighbors;
        this.willingness = willingness;
    }

    public HelloPacket(UUID uuid, IP sourceIp, Set<IP> neighbors, int willingness) {
        super(uuid, sourceIp, IP.BROADCAST);
        this.neighbors = neighbors;
        this.willingness = willingness;
    }

    @Override
    public NetworkPacket copy() {
        return new HelloPacket(getOriginatorIP(), neighbors, willingness);
    }
}
