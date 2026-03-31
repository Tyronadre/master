package de.tyro.mcnetwork.routing.packet.olsr;

import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TCPacket extends NetworkPacket implements IProtocolPaket {
    public final Map<IP, Set<IP>> advertisedLinks;

    public TCPacket(IP sourceIp, Map<IP, Set<IP>> advertisedLinks) {
        super(sourceIp, IP.BROADCAST);
        this.advertisedLinks = advertisedLinks;
    }

    public TCPacket(UUID uuid, IP sourceIp, Map<IP, Set<IP>> advertisedLinks) {
        super(uuid, sourceIp, IP.BROADCAST);
        this.advertisedLinks = advertisedLinks;
    }

    @Override
    public NetworkPacket copy() {
        return new TCPacket(getOriginatorIP(), advertisedLinks);
    }
}
