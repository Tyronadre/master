package de.tyro.mcnetwork.routing.packet;

import java.util.UUID;

public abstract class Packet {
    protected final UUID source;
    protected final UUID destination;
    protected int ttl;

    protected Packet(UUID source, UUID destination,  int ttl) {
        this.source = source;
        this.destination = destination;
    }

    public abstract PacketType getType();

    public UUID getSource() {
        return source;
    }

    public UUID getDestination() {
        return destination;
    }

    public int getTtl() {
        return ttl;
    }
}
