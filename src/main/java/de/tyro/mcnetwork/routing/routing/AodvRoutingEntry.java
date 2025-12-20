package de.tyro.mcnetwork.routing.routing;

import java.util.UUID;

public class AodvRoutingEntry extends RoutingEntry {

    private final int sequenceNumber;
    private final boolean valid;

    public AodvRoutingEntry(UUID destination, UUID nextHop, int cost, int sequenceNumber, long validUntil) {
        super(destination, nextHop, cost, validUntil);
        this.sequenceNumber = sequenceNumber;
        this.valid = true;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public boolean isValid() {
        return valid;
    }
}

