package de.tyro.mcnetwork.simulation;

import de.tyro.mcnetwork.simulation.exceptions.DestinationUnreachableException;
import de.tyro.mcnetwork.simulation.packet.IApplicationPacket;
import de.tyro.mcnetwork.simulation.packet.application.DestinationUnreachablePacket;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class ApplicationMessageBus {

    private final Deque<IApplicationPacket> packets = new ConcurrentLinkedDeque<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition stateChanged = lock.newCondition();
    private final INetworkNode node;

    public ApplicationMessageBus(INetworkNode node) {
        this.node = node;
    }

    /**
     * Neue Pakete vom Netzwerk kommen hier an.
     */
    public void handle(IApplicationPacket packet) {
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
    public <T extends IApplicationPacket> T waitFor(
            Class<T> type,
            Function<T, Boolean> filter,
            long timeoutMs
    ) throws InterruptedException, DestinationUnreachableException {

        SimulationEngine sim = SimulationEngine.getInstance(node.getLevel().isClientSide);
        long startSimTime = sim.getSimTime();
        long deadlineSimTime = startSimTime + timeoutMs;

        lock.lock();
        try {
            while (true) {
                var packet = packets.poll();

                //check destination unreachable packets
                if (packet instanceof DestinationUnreachablePacket du) {
                    throw new DestinationUnreachableException(du.getDestinationIP());
                }

                //check other packets
                if (type.isInstance(packet)) {
                    T cast = type.cast(packet);
                    if (filter.apply(cast)) {
                        return cast;
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

    public void simTick() {
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
    public <T extends IApplicationPacket> void clear(Class<T> packetClass) {
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
    public <T extends IApplicationPacket> List<T> getAll(Class<T> type) {
        lock.lock();
        try {
            List<T> result = new ArrayList<>();
            for (IApplicationPacket p : packets) {
                if (type.isInstance(p)) result.add(type.cast(p));
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
}