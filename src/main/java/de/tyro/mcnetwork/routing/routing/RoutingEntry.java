package de.tyro.mcnetwork.routing.routing;

import java.util.Objects;
import java.util.UUID;

public class RoutingEntry {
    private final UUID destination;
    private final UUID nextHop;
    private final int cost;
    private final long validUntil;

    public RoutingEntry(
            UUID destination,
            UUID nextHop,
            int cost,
            long validUntil
    ) {
        this.destination = destination;
        this.nextHop = nextHop;
        this.cost = cost;
        this.validUntil = validUntil;
    }

    public UUID destination() {
        return destination;
    }

    public UUID nextHop() {
        return nextHop;
    }

    public int cost() {
        return cost;
    }

    public long validUntil() {
        return validUntil;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RoutingEntry) obj;
        return Objects.equals(this.destination, that.destination) &&
                Objects.equals(this.nextHop, that.nextHop) &&
                this.cost == that.cost &&
                this.validUntil == that.validUntil;
    }

    @Override
    public int hashCode() {
        return Objects.hash(destination, nextHop, cost, validUntil);
    }

    @Override
    public String toString() {
        return "RoutingEntry[" +
                "destination=" + destination + ", " +
                "nextHop=" + nextHop + ", " +
                "cost=" + cost + ", " +
                "validUntil=" + validUntil + ']';
    }
}
