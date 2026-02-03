package de.tyro.mcnetwork.routing;

import de.tyro.mcnetwork.routing.packet.NetworkPacket;

import java.util.*;
import java.util.concurrent.locks.*;
import java.util.function.Function;

public class ApplicationMessageBus {

    private final List<NetworkPacket> packets = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition newPacket = lock.newCondition();

    /**
     * Neue Pakete vom Netzwerk kommen hier an.
     */
    public void handle(NetworkPacket packet) {
        lock.lock();
        try {
            packets.add(packet);
            // Benachrichtige alle wartenden Threads sofort
            newPacket.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wartet blockierend auf ein Paket vom Typ T, das das filter-Kriterium erfüllt.
     * Liefert direkt das Paket zurück, löscht es aber nicht aus der Queue.
     */
    public <T extends NetworkPacket> T waitFor(
            Class<T> type,
            Function<T, Boolean> filter,
            long timeoutMs
    ) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        lock.lock();
        try {
            while (true) {
                // Prüfe zuerst vorhandene Pakete
                for (NetworkPacket p : packets) {
                    if (type.isInstance(p) && filter.apply(type.cast(p))) {
                        return type.cast(p);
                    }
                }

                // Berechne Restzeit
                long waitTime = deadline - System.currentTimeMillis();
                if (waitTime <= 0) return null;

                // Warte bis ein neues Paket ankommt oder Timeout
                newPacket.awaitNanos(waitTime * 1_000_000);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Entfernt gezielt Pakete eines Typs
     */
    public <T extends NetworkPacket> void clear(Class<T> packetClass) {
        lock.lock();
        try {
            packets.removeIf(p -> packetClass.isInstance(p));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gibt alle Pakete eines bestimmten Typs zurück, ohne sie zu löschen.
     */
    public <T extends NetworkPacket> List<T> getAll(Class<T> type) {
        lock.lock();
        try {
            List<T> result = new ArrayList<>();
            for (NetworkPacket p : packets) {
                if (type.isInstance(p)) result.add(type.cast(p));
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
}