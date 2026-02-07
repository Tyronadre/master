package de.tyro.mcnetwork.routing;

import de.tyro.mcnetwork.routing.exceptions.DestinationUnreachableException;
import de.tyro.mcnetwork.routing.packet.DestinationUnreachablePacket;
import de.tyro.mcnetwork.routing.packet.IApplicationPaket;

import java.util.*;
import java.util.concurrent.locks.*;
import java.util.function.Function;

public class ApplicationMessageBus {

    private final List<IApplicationPaket> packets = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition stateChanged = lock.newCondition();

    /**
     * Neue Pakete vom Netzwerk kommen hier an.
     */
    public void handle(IApplicationPaket packet) {
        lock.lock();
        try {
            packets.add(packet);
            stateChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wartet blockierend auf ein Paket vom Typ T, das das filter-Kriterium erfüllt.
     * Liefert direkt das Paket zurück, löscht es aber nicht aus der Queue.
     */
    public <T extends IApplicationPaket> T waitFor(
            Class<T> type,
            Function<T, Boolean> filter,
            long timeoutMs
    ) throws InterruptedException, DestinationUnreachableException {

        SimulationEngine sim = SimulationEngine.getInstance();
        long startSimTime = sim.getSimTime();
        long deadlineSimTime = startSimTime + timeoutMs;

        lock.lock();
        try {
            while (true) {


                for (IApplicationPaket p : packets) {
                    //check destination unreachable packets
                    if (p instanceof DestinationUnreachablePacket du) {
                        throw new DestinationUnreachableException(du.getDestinationIP());
                    }

                    //check other packets
                    if (type.isInstance(p)) {
                        T cast = type.cast(p);
                        if (filter.apply(cast)) {
                            return cast;
                        }
                    }
                }

                // check timeout
                long now = sim.getSimTime();
                if (now >= deadlineSimTime) {
                    return null;
                }

                // go back to waiting
                stateChanged.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public void tick() {
        lock.lock();
        try {
            stateChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Entfernt gezielt Pakete eines Typs
     */
    public <T extends IApplicationPaket> void clear(Class<T> packetClass) {
        lock.lock();
        try {
            packets.removeIf(packetClass::isInstance);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gibt alle Pakete eines bestimmten Typs zurück, ohne sie zu löschen.
     */
    public <T extends IApplicationPaket> List<T> getAll(Class<T> type) {
        lock.lock();
        try {
            List<T> result = new ArrayList<>();
            for (IApplicationPaket p : packets) {
                if (type.isInstance(p)) result.add(type.cast(p));
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
}