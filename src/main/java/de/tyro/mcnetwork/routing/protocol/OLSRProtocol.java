package de.tyro.mcnetwork.routing.protocol;

import com.mojang.datafixers.util.Pair;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.olsr.HelloPacket;
import de.tyro.mcnetwork.routing.packet.olsr.TCPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec2;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OLSRProtocol implements IRoutingProtocol {

    // CONSTANTS
    public static final long HELLO_INTERVAL = 2_000; // ms
    public static final long TC_INTERVAL = 5_000; // ms
    public static final int WILLINGNESS = 3; // default willingness

    // LOCAL STATE
    private final INetworkNode node;
    private final SimulationEngine simulator;
    private final Map<IP, NeighborInfo> neighborTable = new ConcurrentHashMap<>();
    private final Map<IP, Set<IP>> twoHopNeighbors = new ConcurrentHashMap<>();
    private final Set<IP> mprs = new HashSet<>();
    private final Map<IP, Set<IP>> topologyTable = new ConcurrentHashMap<>();
    private final Map<IP, RouteEntry> routingTable = new ConcurrentHashMap<>();
    private final Map<IP, List<Pair<INetworkPacket, Integer>>> pendingData = new ConcurrentHashMap<>();
    private long lastHelloTime;
    private long lastTCTime;

    public OLSRProtocol(INetworkNode node) {
        this.node = node;
        this.simulator = SimulationEngine.getInstance(node.getLevel().isClientSide());
        this.lastHelloTime = simulator.getSimTime();
        this.lastTCTime = simulator.getSimTime();
    }

    @Override
    public INetworkNode getNode() {
        return node;
    }

    @Override
    public boolean hasRoute(IP destination) {
        return routingTable.containsKey(destination);
    }

    @Override
    public void discoverRoute(IP destIp) {
        // Proactive, route should already be there
        if (!hasRoute(destIp)) {
            // Buffer
            pendingData.computeIfAbsent(destIp, k -> new ArrayList<>());
        }
    }

    @Override
    public void send(INetworkPacket packet, int ttl) {
        if (packet.getDestinationIP().equals(node.getIP())) {
            // Local delivery
            return;
        }
        if (packet.getDestinationIP().equals(IP.BROADCAST)) {
            simulator.broadcast(node, packet, ttl);
            return;
        }
        if (packet instanceof IProtocolPaket) {
            simulator.broadcast(node, packet, ttl);
            return;
        }
        RouteEntry entry = routingTable.get(packet.getDestinationIP());
        if (entry != null) {
            var nextNode = simulator.getNode(entry.nextHop);
            simulator.unicast(node, nextNode, packet, ttl);
        } else {
            pendingData.computeIfAbsent(packet.getDestinationIP(), k -> new ArrayList<>()).add(Pair.of(packet, ttl));
        }
    }

    @Override
    public void onProtocolPacketReceived(IProtocolPaket packet) {
        if (packet instanceof HelloPacket hello) {
            handleHello(hello);
        } else if (packet instanceof TCPacket tc) {
            handleTC(tc);
        }
    }

    private void handleHello(HelloPacket hello) {
        IP sender = hello.getOriginatorIP();
        neighborTable.put(sender, new NeighborInfo(sender, simulator.getSimTime()));
        // Update 2-hop
        for (IP n : hello.neighbors) {
            if (!n.equals(node.getIP())) {
                twoHopNeighbors.computeIfAbsent(sender, k -> new HashSet<>()).add(n);
            }
        }
        selectMPRs();
    }

    private void handleTC(TCPacket tc) {
        IP sender = tc.getOriginatorIP();
        topologyTable.put(sender, new HashSet<>(tc.advertisedLinks.keySet()));
        computeRoutes();
    }

    private void selectMPRs() {
        mprs.clear();
        // Simple: select all 1-hop as MPRs
        mprs.addAll(neighborTable.keySet());
    }

    private void computeRoutes() {
        // Simple Dijkstra or Floyd-Warshall for shortest paths
        // For simplicity, assume direct links
        routingTable.clear();
        for (IP neighbor : neighborTable.keySet()) {
            routingTable.put(neighbor, new RouteEntry(neighbor, 1, true, Long.MAX_VALUE));
        }
        // Add from topology
        for (Map.Entry<IP, Set<IP>> entry : topologyTable.entrySet()) {
            IP from = entry.getKey();
            for (IP to : entry.getValue()) {
                // Update routing table
                RouteEntry existing = routingTable.get(to);
                if (existing == null || existing.hopCount > 2) {
                    routingTable.put(to, new RouteEntry(from, 2, true, Long.MAX_VALUE));
                }
            }
        }
    }

    @Override
    public void simTick() {
        long now = simulator.getSimTime();
        if (now - lastHelloTime > HELLO_INTERVAL) {
            sendHello();
            lastHelloTime = now;
        }
        if (now - lastTCTime > TC_INTERVAL && mprs.contains(node.getIP())) {
            sendTC();
            lastTCTime = now;
        }
        // Cleanup old neighbors
        neighborTable.entrySet().removeIf(e -> now - e.getValue().lastSeen > 10_000);
    }

    private void sendHello() {
        Set<IP> myNeighbors = new HashSet<>(neighborTable.keySet());
        HelloPacket hello = new HelloPacket(node.getIP(), myNeighbors, WILLINGNESS);
        simulator.broadcast(node, hello, 1);
    }

    private void sendTC() {
        Map<IP, Set<IP>> links = new HashMap<>();
        for (IP n : neighborTable.keySet()) {
            links.put(n, Set.of(node.getIP()));
        }
        TCPacket tc = new TCPacket(node.getIP(), links);
        simulator.broadcast(node, tc, 255);
    }

    @Override
    public void render(RenderUtil renderer) {
        var width = getRenderSize(renderer.getFont()).x;
        float y = 0f;

        renderer.renderHLineWithAlphaColor(width, y);
        y += 4;

        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "OLSR Protocol", width, y);
        y += 9;

        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "Neighbors: " + neighborTable.size(), width, y);
        y += 9;

        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "MPRs: " + mprs.size(), width, y);
        y += 10;

        renderer.renderHLineWithAlphaColor(width, y);
        y += 2;

        renderer.drawString(RenderUtil.Align.CENTER, "Routing Table", 0xAAAAFF, width, y);
        y += 10;

        if (routingTable.isEmpty()) {
            renderer.drawStringWithAlphaColor(RenderUtil.Align.CENTER, "<empty>", width, y);
        } else {
            for (Map.Entry<IP, RouteEntry> entry : routingTable.entrySet()) {
                String line = entry.getKey() + " -> " + entry.getValue().nextHop + " h=" + entry.getValue().hopCount;
                renderer.drawString(RenderUtil.Align.LEFT, line, 0x55FF55, width, y);
                y += 8;
            }
        }
    }

    @Override
    public Vec2 getRenderSize(Font font) {
        var height = 45;
        height += 8 * Math.max(1, routingTable.size());
        return new Vec2(200, height);
    }

    private static class NeighborInfo {
        IP ip;
        long lastSeen;

        NeighborInfo(IP ip, long lastSeen) {
            this.ip = ip;
            this.lastSeen = lastSeen;
        }
    }

    private static class RouteEntry {
        IP nextHop;
        int hopCount;
        boolean valid;
        long lifetime;

        RouteEntry(IP nextHop, int hopCount, boolean valid, long lifetime) {
            this.nextHop = nextHop;
            this.hopCount = hopCount;
            this.valid = valid;
            this.lifetime = lifetime;
        }
    }
}
