package de.tyro.mcnetwork.routing;

import de.tyro.mcnetwork.item.entity.NetworkFrameItemEntity;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
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

    private final List<INetworkNode> nodes = new ArrayList<>();
    private final List<NetworkFrame> frames = new CopyOnWriteArrayList<>();

    private double simulationTimeMs = 0;
    private double simulationSpeed = 1;
    private boolean simulationPaused = false;
    private static final long TICK_DURATION_MS = 50;
    private final double COMM_RADIUS;

    public void registerNode(INetworkNode node) {
        nodes.add(node);
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
                    frame.to.onPacketReceived(frame.packet);
                } catch (Exception e) {
                    log.warn(e.toString());
                } finally {
                    frames.remove(frame);
                }
            }
        }

        for (INetworkNode node : nodes) {
            node.tick();
        }
    }

    public List<INetworkNode> getNeighbors(INetworkNode node) {
        return nodes.stream().filter(n -> n != node).filter(n -> distance(node, n) <= COMM_RADIUS).toList();
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

    public boolean sendUnicast(INetworkNode from, INetworkNode to, INetworkPacket packet, int ttl) {
        if (ttl <= 0) return false;
        if (from == null || to == null) return false;
        if (distance(from, to) > COMM_RADIUS) return false; // out of range
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

    private double distance(INetworkNode a, INetworkNode b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public INetworkNode getNodeByIp(IP nextHop) {
        return nodes.stream().filter(it -> it.getIP() == nextHop).findFirst().orElse(null);
    }

    public List<INetworkNode> getNodeList() {
        return nodes;
    }

    public void unregisterNode(INetworkNode node) {
        nodes.remove(node);
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
}