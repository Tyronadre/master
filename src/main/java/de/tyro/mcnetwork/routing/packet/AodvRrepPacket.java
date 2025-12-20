package de.tyro.mcnetwork.routing.packet;

import java.util.UUID;

public class AodvRrepPacket extends Packet {

    private final int destinationSequenceNumber;
    private int hopCount;

    public AodvRrepPacket(UUID source, UUID destination, int destSeq, int ttl) {
        super(source, destination, ttl);
        this.destinationSequenceNumber = destSeq;
        this.hopCount = 0;
    }

    public int getDestinationSequenceNumber() {
        return destinationSequenceNumber;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void incrementHopCount() {
        hopCount++;
    }

    @Override
    public PacketType getType() {
        return PacketType.RREP;
    }
}
