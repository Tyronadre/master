package de.tyro.mcnetwork.simulation.protocol;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TickActions {
    private static final Logger log = LogUtils.getLogger();

    private final List<TickAction> actions = new ArrayList<>();
    private final java.util.concurrent.locks.ReentrantLock lock =
            new java.util.concurrent.locks.ReentrantLock();

    public int size() {
        lock.lock();
        try {
            return actions.size();
        } finally {
            lock.unlock();
        }
    }

    public void add(long tick, Runnable task) {
        log.debug("Adding TickAction on {} in {}", tick, Integer.toHexString(this.hashCode()));
        lock.lock();
        try {
            actions.add(new TickActions.TickAction(tick, task));
        } finally {
            lock.unlock();
        }
    }

    public void execute(long currentTick) {
        List<TickAction> toExecute = new ArrayList<>();

        lock.lock();
        try {
            Iterator<TickAction> iterator = actions.iterator();
            while (iterator.hasNext()) {
                TickActions.TickAction action = iterator.next();
                if (action.tick() <= currentTick) {
                    toExecute.add(action);
                    iterator.remove();
                }
            }
        } finally {
            lock.unlock();
        }

        for (TickAction action : toExecute) {
            try {
                action.run();
            } catch (Exception e) {
                log.error("Error executing TickAction", e);
            }
        }
    }

    private record TickAction(long tick, Runnable task) {
        public void run() {
            task.run();
        }
    }

    @Override
    public String toString() {
        return TickActions.class.getSimpleName() + "[actions=" + size() + "]";
    }
}
