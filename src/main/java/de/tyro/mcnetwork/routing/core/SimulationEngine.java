package de.tyro.mcnetwork.routing.core;

import de.tyro.mcnetwork.routing.event.SimulationEvent;
import de.tyro.mcnetwork.routing.node.SimNode;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class SimulationEngine {
    public static final SimulationEngine INSTANCE = new SimulationEngine(10);

    private final Queue<SimulationEvent> eventQueue = new ArrayDeque<>();
    private final TopologyManager topologyManager;
    private final List<SimNode> nodes = new ArrayList<>();

    private SimulationMode mode = SimulationMode.PAUSED;

    private SimulationEngine(double radioRange) {
        NeoForge.EVENT_BUS.register(this);

        this.topologyManager = new TopologyManager(radioRange);
    }

    public void addNode(SimNode node) {
        nodes.add(node);
    }

    public List<SimNode> getNeighbors(SimNode node) {
        return topologyManager.getNeighbors(node, nodes);
    }

    public void enqueue(SimulationEvent event) {
        eventQueue.add(event);
    }

    public void stepOnce() {
        if (!eventQueue.isEmpty()) {
            eventQueue.poll().execute();
        }
    }

    public void setRunning(boolean running) {
        mode = running ? SimulationMode.RUNNING : SimulationMode.PAUSED;
    }

    public void tick() {
        if (mode == SimulationMode.RUNNING) {
            stepOnce();
        }
    }

    public SimulationMode getMode() {
        return mode;
    }

    public SimNode getNodeById(UUID uuid) {
        return nodes.stream().filter(it -> it.getId() == uuid).findFirst().orElse(null);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        SimulationRegistry.getEngine().tick();
    }
}
