package de.tyro.mcnetwork.routing.routing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoutingTable<T extends RoutingEntry> {

    private final Map<UUID, T> entries = new HashMap<>();

    public void update(T entry) {
        entries.put(entry.destination(), entry);
    }

    public Collection<T> getEntries() {
        return entries.values();
    }

    public T getEntry(UUID id) {
        return entries.get(id);
    }
}
