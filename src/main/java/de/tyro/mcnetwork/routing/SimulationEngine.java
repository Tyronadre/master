package de.tyro.mcnetwork.routing;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.item.entity.PacketItemEntity;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimulationEngine {

    public static final SimulationEngine INSTANCE = new SimulationEngine(10);

    public static SimulationEngine getInstance() {
        return INSTANCE;
    }

    private SimulationEngine(double commRadius) {
        COMM_RADIUS = commRadius;
    }

    private final List<INetworkNode> nodes = new ArrayList<>();
    private final List<InFlightPacket> packets = new CopyOnWriteArrayList<>();

    private long simulationTimeMs = 0;
    private static final long TICK_DURATION_MS = 50;
    private final double COMM_RADIUS;

    public void registerNode(INetworkNode node) {
        nodes.add(node);
    }

    public long getSimTime() {
        return simulationTimeMs;
    }

    @SubscribeEvent
    public void onClientTickEvent(ClientTickEvent.Pre event) {
        simulationTimeMs += TICK_DURATION_MS;


        for (var p : packets) {
            if (p.tick()) {
                p.to.onPacketReceived(p.packet);
                packets.remove(p);
            }
        }

        for (INetworkNode node : nodes) {
            node.getRoutingProtocol().tick(node);
        }
    }

    public List<INetworkNode> getNeighbors(INetworkNode node) {
        return nodes.stream().filter(n -> n != node).filter(n -> distance(node, n) <= COMM_RADIUS).toList();
    }

    public void broadcast(INetworkNode from, INetworkPacket packet) {
        for (INetworkNode n : getNeighbors(from)) {
            newPacket(from, n, packet);
        }
    }

    public void sendUnicast(INetworkNode from, INetworkNode to, INetworkPacket packet) {
        if (distance(from, to) > COMM_RADIUS) return; // out of range
        newPacket(from, to, packet);
    }

    private void newPacket(INetworkNode from, INetworkNode to, INetworkPacket packet) {
        var level = Minecraft.getInstance().level;
        var inFlightPacket = new InFlightPacket(from, to, packet);
        packets.add(inFlightPacket);


        if (level != null && level.isClientSide) {
            PacketItemEntity entity = new PacketItemEntity(level, from.getX() + 0.5, from.getY() + .5, from.getZ() + 0.5, inFlightPacket);
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

    public Iterable<InFlightPacket> getInFlightPackets() {
        return packets;
    }
}