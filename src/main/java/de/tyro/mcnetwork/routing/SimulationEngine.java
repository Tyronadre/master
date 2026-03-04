package de.tyro.mcnetwork.routing;

import de.tyro.mcnetwork.entity.NetworkFrameEntity;
import de.tyro.mcnetwork.network.payload.networkPacket.NetworkPacketCodecRegistry;
import de.tyro.mcnetwork.routing.packet.IApplicationPacket;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimulationEngine {

    private static final SimulationEngine SERVER_INSTANCE = new SimulationEngine(false);
    private static final SimulationEngine CLIENT_INSTANCE = new SimulationEngine(true);

    private static final Logger log = LogManager.getLogger(SimulationEngine.class);
    private final boolean isClientSide;

    public static SimulationEngine getInstance(Boolean clientSide) {
        return clientSide ? CLIENT_INSTANCE : SERVER_INSTANCE;
    }


    private SimulationEngine(boolean isClientSide) {
        this.commRadius = 20;
        this.isClientSide = isClientSide;
    }


    private final Map<IP, INetworkNode> nodes = new ConcurrentHashMap<>();
    private final List<NetworkFrameEntity> networkFrames = new ArrayList<>();

    private long simulationTimeNS = 0;
    private double simulationSpeed = 0.25;
    private boolean simulationPaused = false;
    public static long SIM_TICK_PER_GAME_TICK = 5;
    public static long NS_PER_SIM_TICK = 1;
    private double commRadius;

    public void registerNode(INetworkNode node) {
        log.debug("Registering node {}. {} @ {}", node, node.getLevel(), Integer.toHexString(hashCode()));

        nodes.put(node.getIP(), node);
    }

    public void registerNetworkFrame(NetworkFrameEntity entity) {
        networkFrames.add(entity);
    }

    public long getSimTime() {
        return simulationTimeNS;
    }


    @SubscribeEvent
    public void onClientTickEvent(ClientTickEvent.Pre event) {
        if (!isClientSide) return;
        tick();
    }

    @SubscribeEvent
    public void onServerTickEvent(ServerTickEvent.Pre event) {
        if (isClientSide) return;
        tick();
    }

    private void tick() {
        if (simulationPaused) return;

        // SAFE ACTUAL SIM TIME EVER SUBTICK!
        double simulationTicks = SIM_TICK_PER_GAME_TICK * simulationSpeed;

        for (double i = 0; i < simulationTicks; i++) {
            new ArrayList<>(networkFrames).forEach(NetworkFrameEntity::simTick);
            nodes.values().forEach(INetworkNode::tick);

            simulationTimeNS += NS_PER_SIM_TICK;
        }
    }

    public List<INetworkNode> getNeighbors(INetworkNode node) {
        return nodes.values().stream().filter(n -> !n.getIP().equals(node.getIP())).filter(n -> node.distanceTo(n) <= commRadius).toList();
    }

    /**
     * Broadcasts this packet to all of the neighbors of this node. The packet will be wrapped in a NetworkFrame that will be send.
     * <br>
     * If this is called on the client side, the method will behave the same, but not send any frames.
     *
     * @param from   the where the packet originates
     * @param packet the packet to broadcast
     * @param ttl    the time to life of the packet
     */
    public void broadcast(INetworkNode from, INetworkPacket packet, int ttl) {
        if (from.getLevel().isClientSide()) return;
        if (ttl <= 0) return;
        for (INetworkNode to : getNeighbors(from)) {
            log.debug("Broadcasting {} from {} to {}", packet, from, to);
            newPacket(from, to, packet, ttl);
        }
    }

    /**
     * Unicasts a packet from a node to another node. The packet will be wrapped in a NetworkFrame that will be send.
     * If the ttl is 0 the method will return false and not send a frame.
     * If the destination is out of communication range, this method will return false and not send a frame.
     * If either the source or destination node is null, this method will return false and not send a frame.
     * <br>
     * If this is called on the client side, the method will behave the same, but not send any frames.
     *
     * @param from   the source node
     * @param to     the destination node
     * @param packet the packet to send
     * @param ttl    time to live
     * @return true if the frame was send, false otherwise
     */
    public boolean unicast(INetworkNode from, INetworkNode to, INetworkPacket packet, int ttl) {
        if (ttl <= 0) return false;
        if (from == null || to == null) return false;
        if (from.distanceTo(to) > commRadius) return false; // out of range
        if (from.getLevel().isClientSide()) return true;
        newPacket(from, to, packet, ttl);
        return true;
    }

    /**
     * Always called from server thread only
     *
     * @param from   sending node
     * @param to     receiving node
     * @param packet packet that will be transmitted
     * @param ttl    ttl for the packet
     */
    private void newPacket(INetworkNode from, INetworkNode to, INetworkPacket packet, int ttl) {

        if (from.getIP() == to.getIP()) {
            if (packet instanceof IApplicationPacket p)
                from.onApplicationPacketReceived(p);
            else {
                log.warn("Trying to sending Protocol packet to self {}", packet);
            }
            return;
        }

        log.debug("New packet from {} to {}: {}", from, to, packet);

        if (!NetworkPacketCodecRegistry.isRegistered(packet.getClass())) {
            log.warn("Cannot send unregistered packet {}", packet);
            return;
        }
//        PacketDistributor.sendToSelf(new NewNetworkFramePayload(from.getBlockPos(), to.getBlockPos(), packet, ttl));
        from.getLevel().addFreshEntity(new NetworkFrameEntity(from.getLevel(), from, to, ttl, packet));
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
        log.info(String.format("Setting simulation speed to: %f", speed));
    }

    public boolean nodeExists(IP ip) {
        return nodes.containsKey(ip);
    }

    public void setSimTime(long simulationTime) {
        this.simulationTimeNS = simulationTime;
    }

    public void setCommRadius(double commRadius) {
        this.commRadius = commRadius;
        log.info(String.format("Setting comm radius to: %f", commRadius));
    }

    public double getCommRange() {
        return commRadius;
    }

    public void removeNetworkFrame(NetworkFrameEntity networkFrameEntity) {
        networkFrames.remove(networkFrameEntity);
    }
}