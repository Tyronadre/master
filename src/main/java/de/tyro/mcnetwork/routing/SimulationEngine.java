package de.tyro.mcnetwork.routing;

import de.tyro.mcnetwork.item.entity.NetworkFrameItemEntity;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimulationEngine {

    public static final SimulationEngine INSTANCE = new SimulationEngine(10);
    private static final Logger log = LogManager.getLogger(SimulationEngine.class);

    public static SimulationEngine getInstance() {
        return INSTANCE;
    }

    private SimulationEngine(double commRadius) {
        COMM_RADIUS = commRadius;
    }

    private final Map<IP, INetworkNode> nodes = new ConcurrentHashMap<>();
    private final List<NetworkFrame> frames = new CopyOnWriteArrayList<>();

    private double simulationTimeMs = 0;
    private double simulationSpeed = 1;
    private boolean simulationPaused = false;
    private static final long TICK_DURATION_MS = 50;
    private final double COMM_RADIUS;

    public void registerNode(INetworkNode node) {
        nodes.put(node.getIP(), node);
    }

    public long getSimTime() {
        return ((long) simulationTimeMs);
    }

    public double getExactSimTime() {
        return simulationTimeMs;
    }

    @SubscribeEvent
    public void onClientTickEvent(ClientTickEvent.Pre event) {
        if (simulationPaused) return;

        simulationTimeMs += TICK_DURATION_MS * simulationSpeed;

        for (var frame : frames) {
            if (frame.tick()) {
                try {
                    frame.to.onFrameReceive(frame);
                } catch (Exception e) {
                    log.warn(e.toString());
                } finally {
                    frames.remove(frame);
                }
            }
        }

        for (INetworkNode node : nodes.values()) {
            node.tick();
        }
    }

    public List<INetworkNode> getNeighbors(INetworkNode node) {
        return nodes.values().stream().filter(n -> n != node).filter(n -> node.distanceTo(n) <= COMM_RADIUS).toList();
    }

    /**
     * Broadcasts this packet to all of the neighbors of this node
     *
     * @param from   the where the packet originates
     * @param packet the packet to broadcast
     * @param ttl    the time to life of the packet
     */
    public void broadcast(INetworkNode from, INetworkPacket packet, int ttl) {
        if (ttl <= 0) return;
        for (INetworkNode n : getNeighbors(from)) {
            newPacket(from, n, packet, ttl);
        }
    }

    public boolean unicast(INetworkNode from, INetworkNode to, INetworkPacket packet, int ttl) {
        if (ttl <= 0) return false;
        if (from == null || to == null) return false;
        if (from.distanceTo(to) > COMM_RADIUS) return false; // out of range
        newPacket(from, to, packet, ttl);
        return true;
    }

    private void newPacket(INetworkNode from, INetworkNode to, INetworkPacket packet, int ttl) {
        var level = Minecraft.getInstance().level;
        var inFlightPacket = new NetworkFrame(from, to, packet, ttl);
        frames.add(inFlightPacket);


        if (level != null && level.isClientSide) {
            NetworkFrameItemEntity entity = new NetworkFrameItemEntity(level, from.getX() + 0.5, from.getY() + .5, from.getZ() + 0.5, inFlightPacket);
            level.addEntity(entity);
        }
    }

    /**
     * Finds the node with the given ip, or null if it does not exist
     * @param ip the ip
     * @return the node or null
     */
    public INetworkNode getNode(IP ip) {
        return nodes.get(ip);
    }

    public Collection<INetworkNode> getNodeList() {
        return nodes.values();
    }

    public void unregisterNode(INetworkNode node) {
        nodes.remove(node.getIP());
    }

    public double getSimSpeed() {
        return simulationSpeed;
    }

    public boolean isPaused() {
        return simulationPaused;
    }

    public void setPaused(boolean paused) {
        simulationPaused = paused;
    }

    public void setSimSpeed(double speed) {
        simulationSpeed = speed;
        System.out.println("set sim speed : " + simulationSpeed);
    }

    public boolean nodeExists(IP ip) {
        return nodes.containsKey(ip);
    }
}