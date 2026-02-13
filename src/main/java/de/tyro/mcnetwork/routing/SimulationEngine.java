package de.tyro.mcnetwork.routing;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.entity.NetworkFrameEntity;
import de.tyro.mcnetwork.network.payload.routing.NetworkPacketCodecRegistry;
import de.tyro.mcnetwork.network.payload.routing.NewNetworkFramePayload;
import de.tyro.mcnetwork.routing.packet.IApplicationPaket;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private double simulationTimeMs = 0;
    private double simulationSpeed = 0.05;
    private double frameMovementPerTick = 1;
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

        nodes.values().forEach(INetworkNode::tick);
    }

    @SubscribeEvent
    public void onServerTickEvent(ServerTickEvent.Pre event) {
        if (simulationPaused) return;

        simulationTimeMs += TICK_DURATION_MS * simulationSpeed;

        nodes.values().forEach(INetworkNode::tick);
    }

    public List<INetworkNode> getNeighbors(INetworkNode node) {
        return nodes.values().stream().filter(n -> !n.getIP().equals(node.getIP())).filter(n -> node.distanceTo(n) <= COMM_RADIUS).toList();
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
        for (INetworkNode to : getNeighbors(from)) {
            log.debug("Broadcasting {} from {} to {}", packet, from, to);
            newPacket(from, to, packet, ttl);
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

        if (from.getIP() == to.getIP()) {
            if (packet instanceof IApplicationPaket p)
                from.onApplicationPacketReceived(p);
            else {
                log.warn("Sending Protocol packet to self {}", packet);
                try {
                    throw new Exception();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            return;
        }

        log.debug("New packet from {} to {}: {}", from, to, packet);

        if (!NetworkPacketCodecRegistry.isRegistered(packet.getClass())) {
            log.warn("Cannot send unregistered packet {}", packet);
            return;
        }
        PacketDistributor.sendToServer(new NewNetworkFramePayload(from.getBlockPos(), to.getBlockPos(), packet, ttl));
    }

    /**
     * Finds the node with the given ip, or null if it does not exist
     *
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
        MCNetwork.LOGGER.info(String.format("Setting simulation speed to: %f", speed));
    }

    public boolean nodeExists(IP ip) {
        return nodes.containsKey(ip);
    }

    public double getFrameMovementPerTick() {
        return frameMovementPerTick;
    }

    public void setFrameMovementPerTick(double movementPerTick) {
        this.frameMovementPerTick = movementPerTick;
    }

    public void setSimTime(double simulationTime) {
        this.simulationTimeMs = simulationTime;
    }

}