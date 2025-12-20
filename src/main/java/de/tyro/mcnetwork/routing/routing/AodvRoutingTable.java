package de.tyro.mcnetwork.routing.routing;

import java.util.UUID;

public class AodvRoutingTable extends RoutingTable<AodvRoutingEntry> {

    public boolean hasValidRoute(UUID destination) {
        RoutingEntry entry = getEntry(destination);
        return entry != null && entry.validUntil() > System.currentTimeMillis();
    }
}

