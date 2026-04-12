package de.tyro.mcnetwork.routing.protocol;

import com.mojang.datafixers.util.Pair;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.routing.packet.IApplicationPacket;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.lar.LARRouteError;
import de.tyro.mcnetwork.routing.packet.lar.LARRouteReply;
import de.tyro.mcnetwork.routing.packet.lar.LARRouteRequest;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LARProtocol implements IRoutingProtocol {

    // CONSTANTS
    public static final int REQUEST_ZONE_SIZE = 50; // example size
    public static final int EXPECTED_ZONE_SIZE = 20; // example size

    // LOCAL STATE
    private final INetworkNode node;
    private final SimulationEngine simulator;
    private final Map<IP, RouteEntry> routingTable = new ConcurrentHashMap<>();
    private final Map<IP, LocationEntry> locationTable = new ConcurrentHashMap<>(); // last known locations with timestamps
    private final Map<IP, List<Pair<INetworkPacket, Integer>>> pendingData = new ConcurrentHashMap<>();

    // For rendering zones
    public Vec3 currentRequestZoneMin;
    public Vec3 currentRequestZoneMax;
    public Vec3 currentExpectedZoneCenter;
    public int currentRequestZoneSize;
    public int currentExpectedZoneSize;

    public LARProtocol(INetworkNode node) {
        this.node = node;
        this.simulator = SimulationEngine.getInstance(node.getLevel().isClientSide());
    }

    @Override
    public INetworkNode getNode() {
        return node;
    }

    @Override
    public boolean hasRoute(IP destination) {
        RouteEntry entry = routingTable.get(destination);
        return entry != null && entry.valid;
    }

    @Override
    public void discoverRoute(IP destIp) {
        if (hasRoute(destIp)) return;

        Vec3 sourcePos = node.getPos();
        LocationEntry destEntry = locationTable.get(destIp);
        Vec3 destExpectedPos;
        double expectedZoneRadius = EXPECTED_ZONE_SIZE; // fixed for simplicity

        if (destEntry != null) {
            destExpectedPos = destEntry.position;
            // In standard LAR, expected zone radius = speed * time since last update
            // For simplicity, use fixed
        } else {
            destExpectedPos = sourcePos; // fallback
        }

        // LAR1 Request Zone: rectangle from source to expected zone center, expanded by expected zone radius
        double minX = Math.min(sourcePos.x, destExpectedPos.x) - expectedZoneRadius;
        double maxX = Math.max(sourcePos.x, destExpectedPos.x) + expectedZoneRadius;
        double minZ = Math.min(sourcePos.z, destExpectedPos.z) - expectedZoneRadius;
        double maxZ = Math.max(sourcePos.z, destExpectedPos.z) + expectedZoneRadius;

        // Set zone for rendering
        currentRequestZoneMin = new Vec3(minX, sourcePos.y - 10, minZ); // some height
        currentRequestZoneMax = new Vec3(maxX, sourcePos.y + 10, maxZ);
        currentExpectedZoneCenter = destExpectedPos;
        currentRequestZoneSize = (int) Math.max(maxX - minX, maxZ - minZ);
        currentExpectedZoneSize = (int) expectedZoneRadius;

        LARRouteRequest rreq = new LARRouteRequest(node.getIP(), destIp, sourcePos, destExpectedPos, (int) expectedZoneRadius);
        // Broadcast (in real LAR, only to nodes in request zone, but for sim, broadcast)
        simulator.broadcast(node, rreq, 10);
    }

    @Override
    public void send(INetworkPacket packet, int ttl) {
        //if the destination is this node, we send a packet to ourself. this can only be an application packet. receive it or throw an error
        if (packet.getDestinationIP().equals(node.getIP())) {
            if (packet instanceof IApplicationPacket a) {
                node.onApplicationPacketReceived(a);
                return;
            } else {
                throw new IllegalArgumentException("Trying to send a packet of type " + packet.getClass().getSimpleName() + " to its own node");
            }
        }

        //if the destination of the packet is the broadcast address, do that!
        if (packet.getDestinationIP().equals(IP.BROADCAST)) {
            simulator.broadcast(node, packet, ttl);
            return;
        }

        // For protocol packets, broadcast
        if (packet instanceof IProtocolPaket) {
            simulator.broadcast(node, packet, ttl);
            return;
        }

        //we want to send this somewhere else. check if we have a destination, and either send it, or store it for later
        var entry = routingTable.get(packet.getDestinationIP());
        if (hasRoute(packet.getDestinationIP())) {
            var nextNode = simulator.getNode(entry.nextHop);
            if (simulator.unicast(node, nextNode, packet, 100)) {
                entry.lifetime = simulator.getSimTime() + 3000; // extend lifetime
            }
            return;
        }

        //buffer the packet for later
        pendingData.computeIfAbsent(packet.getDestinationIP(), k -> new ArrayList<>()).add(Pair.of(packet, ttl));
        discoverRoute(packet.getDestinationIP());
    }

    @Override
    public void onProtocolPacketReceived(IProtocolPaket packet) {
        if (packet instanceof LARRouteRequest rreq) {
            handleRREQ(rreq);
        } else if (packet instanceof LARRouteReply rrep) {
            handleRREP(rrep);
        } else if (packet instanceof LARRouteError rerr) {
            handleRERR(rerr);
        }
    }

    private void handleRREQ(LARRouteRequest rreq) {
        Vec3 myPos = node.getPos();
        double expectedRadius = rreq.requestZoneSize;

        // LAR1 Request Zone check
        double minX = Math.min(rreq.sourcePos.x, rreq.destExpectedPos.x) - expectedRadius;
        double maxX = Math.max(rreq.sourcePos.x, rreq.destExpectedPos.x) + expectedRadius;
        double minZ = Math.min(rreq.sourcePos.z, rreq.destExpectedPos.z) - expectedRadius;
        double maxZ = Math.max(rreq.sourcePos.z, rreq.destExpectedPos.z) + expectedRadius;

        boolean inRequestZone = myPos.x >= minX && myPos.x <= maxX && myPos.z >= minZ && myPos.z <= maxZ;

        if (inRequestZone && rreq.hopCount < 10) {
            rreq.hopCount++;
            // Forward or reply
            if (node.getIP().equals(rreq.getDestinationIP())) {
                // Reply
                LARRouteReply rrep = new LARRouteReply(rreq.getDestinationIP(), rreq.getOriginatorIP(), myPos, rreq.sourcePos);
                send(rrep, 10);
            } else {
                // Forward if in forwarding zone (closer to destination than source)
                double distToDest = myPos.distanceTo(rreq.destExpectedPos);
                double sourceDistToDest = rreq.sourcePos.distanceTo(rreq.destExpectedPos);
                if (distToDest < sourceDistToDest) {
                    send(rreq, 10);
                }
            }
        }
    }

    private void handleRREP(LARRouteReply rrep) {
        // Update routing table
        routingTable.put(rrep.getOriginatorIP(), new RouteEntry(rrep.getOriginatorIP(), 1, true, simulator.getSimTime() + 3000));
        locationTable.put(rrep.getOriginatorIP(), new LocationEntry(rrep.sourcePos, simulator.getSimTime()));
        // Send pending data
    }

    private void handleRERR(LARRouteError rerr) {
        // Invalidate route
        RouteEntry entry = routingTable.get(rerr.unreachableDestination);
        if (entry != null) {
            entry.valid = false;
        }
    }

    @Override
    public void simTick() {
        // Cleanup expired routes
    }

    @Override
    public void render(RenderUtil renderer) {
        var width = getRenderSize(renderer.getFont()).x;
        float y = 0f;

        renderer.renderHLineWithAlphaColor(width, y);
        y += 4;

        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "LAR Protocol", width, y);
        y += 9;

        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "Request Zone Size: " + REQUEST_ZONE_SIZE, width, y);
        y += 9;

        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "Expected Zone Size: " + EXPECTED_ZONE_SIZE, width, y);
        y += 10;

        renderer.renderHLineWithAlphaColor(width, y);
        y += 2;

        renderer.drawString(RenderUtil.Align.CENTER, "Location Table", 0xAAAAFF, width, y);
        y += 10;

        if (locationTable.isEmpty()) {
            renderer.drawStringWithAlphaColor(RenderUtil.Align.CENTER, "<empty>", width, y);
            y += 8;
        } else {
            for (Map.Entry<IP, LocationEntry> entry : locationTable.entrySet()) {
                String line = entry.getKey() + " @ " + entry.getValue().position + " (last seen: " + entry.getValue().timestamp + ")";
                renderer.drawString(RenderUtil.Align.LEFT, line, 0x55FF55, width, y);
                y += 8;
            }
        }
    }

    @Override
    public Vec2 getRenderSize(Font font) {
        var height = 35;
        height += 8 * Math.max(1, locationTable.size());
        return new Vec2(200, height);
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

    private static class LocationEntry {
        Vec3 position;
        long timestamp;

        LocationEntry(Vec3 position, long timestamp) {
            this.position = position;
            this.timestamp = timestamp;
        }
    }

    @Override
    public Map<String, Object> getSettings() {
        return Map.of();
    }

    @Override
    public void setSetting(String key, Object value) {
        // No settings to set
    }
}
