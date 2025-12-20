package de.tyro.mcnetwork.routing.packet;

import java.util.UUID;

public class AodvRreqPacket extends Packet {

    private final int requestId;
    private final int sourceSequenceNumber;
    private int hopCount;

    public AodvRreqPacket(UUID source, UUID destination, int requestId, int sourceSeq, int ttl) {
        super(source, destination, ttl);
        this.requestId = requestId;
        this.sourceSequenceNumber = sourceSeq;
        this.hopCount = 0;
    }

    public void incrementHopCount() {
        hopCount++;
    }

    public int getHopCount() {
        return hopCount;
    }

    public int getRequestId() {
        return requestId;
    }

    public int getSourceSequenceNumber() {
        return sourceSequenceNumber;
    }

    @Override
    public PacketType getType() {
        return PacketType.RREQ;
    }
}

