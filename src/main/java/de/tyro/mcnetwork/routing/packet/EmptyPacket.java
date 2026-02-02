package de.tyro.mcnetwork.routing.packet;

import java.util.UUID;

public class EmptyPacket extends Packet {

    public EmptyPacket(UUID source, UUID destination, int ttl) {
        super(source, destination, ttl);
    }

    @Override
    public PacketType getType() {
        return PacketType.EMPTY;
    }

    @Override
    public Packet copy() {
        return new EmptyPacket(source, destination, ttl);
    }
}
