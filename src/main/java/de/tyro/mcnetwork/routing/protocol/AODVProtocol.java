package de.tyro.mcnetwork.routing.protocol;

import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.routing.packet.AODVRREPPacket;
import de.tyro.mcnetwork.routing.packet.AODVRREQPacket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AODVProtocol implements RoutingProtocol {
    //CONSTANTS
    private static final SimulationEngine simulator = SimulationEngine.getInstance();

    private static final long ACTIVE_ROUTE_TIMEOUT = 5_000_000;
    private static final long NET_TRAVERSAL_TIME = 2_000_000;
    private static final long PATH_DISCOVERY_TIME = 3_000_000;

    //LOCAL STATE
    private int sequenceNumber = 0;
    private int rreqId = 0;
    private final Map<IP, RouteEntry> routingTable = new HashMap<>();
    private final Map<String, Long> seenRreqs = new HashMap<>(); // originIP + RREQ_ID -> Arrival Time
    private final Map<IP, List<NetworkPacket>> pendingData = new HashMap<>();


    @Override
    public boolean hasRoute(IP destination) {
        var entry = routingTable.get(destination);
        return entry != null && entry.valid && entry.lifetime > simulator.getSimTime();
    }

    @Override
    public Collection<String> renderData() {
        return routingTable.entrySet().stream().map(it ->
                it.getKey() + " | " + it.getValue().nextHop + " @ " + it.getValue().seqNumber).toList();
    }

    @Override
    public void onSendRequest(INetworkNode self, IP destIp) {
        if (hasRoute(destIp)) return;

        sequenceNumber++;
        rreqId++;

        var entry = routingTable.get(destIp);
        int destSeq = (entry != null && entry.seqValid) ? entry.seqNumber : 0;

        var rreq = new AODVRREQPacket(self.getIP(), destIp, self.getIP(), entry == null, destSeq, sequenceNumber, rreqId, 0);
        seenRreqs.put(key(self.getIP(), rreqId), simulator.getSimTime());

        simulator.broadcast(self, rreq);
    }

    @Override
    public void onPacketReceived(INetworkNode self, NetworkPacket packet) {

        //Handle AODV Packets first
        if (packet instanceof AODVRREQPacket rreq) {
            handleRREQ(self, rreq);
            return;
        }

        if (packet instanceof AODVRREPPacket rrep) {
            handleRREP(self, rrep);
            return;
        }

        //now we handle all other packets that have this node as destination
        if (self.getIP().equals(packet.destinationIp)) {
            self.onApplicationPacketReceived(packet);
            return;
        }

        //Unicast the package to the next hop
        sendData(self, packet);
    }

    private void handleRREP(INetworkNode self, AODVRREPPacket rrep) {
        rrep.hopCount++;

        updateForwardRoute(rrep.sourceIp, rrep.previousHopIP, rrep.hopCount, rrep.destSeqNumber, rrep.lifetime);

        //we got an answer to request
        if (self.getIP().equals(rrep.destinationIp)) return;

        sendUnicast(self, rrep.destinationIp, rrep.hop(self));
    }

    private void updateForwardRoute(IP dest, IP nextHop, int hopCount, int destSeqNumber, long lifetime) {
        var entry = routingTable.computeIfAbsent(dest, k -> new RouteEntry(dest));

        if (!entry.valid || !entry.seqValid || destSeqNumber > entry.seqNumber || (destSeqNumber == entry.seqNumber && hopCount < entry.hopCount)) {
            entry.nextHop = nextHop;
            entry.hopCount = hopCount;
            entry.seqNumber = destSeqNumber;
            entry.seqValid = true;
            entry.valid = true;
            entry.lifetime = simulator.getSimTime() + lifetime;
        }
    }

    private void handleRREQ(INetworkNode self, AODVRREQPacket rreq) {
        String key = key(rreq.sourceIp, rreq.rreqId);
        if (seenRreqs.containsKey(key)) return;
        seenRreqs.put(key, simulator.getSimTime());
        rreq.hopCount++;

        updateReverseRoute(rreq.sourceIp, rreq.previousHopIP, rreq.hopCount, rreq.originatorSequenceNumber);

        //this node is the destination
        if (self.getIP().equals(rreq.destinationIp)) {
            sequenceNumber = Math.max(sequenceNumber, rreq.destinationSequenceNumber);
            sequenceNumber++;

            var rrep = new AODVRREPPacket(self.getIP(), rreq.sourceIp, self.getIP(), false, false, 0, rreq.originatorSequenceNumber, simulator.getSimTime() + ACTIVE_ROUTE_TIMEOUT);

            sendUnicast(self, rreq.sourceIp, rrep);
            return;
        }

        //this node is not the destination, but we have a route in our table to that destination.
        var entry = routingTable.get(rreq.destinationIp);
        if (entry != null && entry.valid && entry.seqNumber >= rreq.destinationSequenceNumber) {
            AODVRREPPacket rrep = new AODVRREPPacket(rreq.destinationIp, rreq.sourceIp, self.getIP(), false, false, entry.hopCount, entry.seqNumber, entry.lifetime - simulator.getSimTime());

            sendUnicast(self, rreq.sourceIp, rrep);
            return;
        }

        //this node doesnt know anything. broadcast
        simulator.broadcast(self, rreq.hop(self));
    }

    private void sendUnicast(INetworkNode self, IP destination, NetworkPacket packet) {
        var entry = routingTable.get(destination);
        if (entry != null && entry.valid) simulator.sendUnicast(self, simulator.getNodeByIp(entry.nextHop), packet);
    }

    private void updateReverseRoute(IP origin, IP nextHop, int hops, int seq) {
        RouteEntry entry = routingTable.computeIfAbsent(origin, k -> new RouteEntry(origin));
        entry.nextHop = nextHop;
        entry.hopCount = hops;
        entry.seqNumber = seq;
        entry.valid = true;
        entry.seqValid = true;
        entry.seqNumber++;
        entry.lifetime = simulator.getSimTime() + ACTIVE_ROUTE_TIMEOUT;
    }


    private static String key(IP ip, int id) {
        return ip.toString() + ":" + id;
    }

    @Override
    public void sendData(INetworkNode self, NetworkPacket packet) {
        if (hasRoute(packet.destinationIp)) {
            forwardData(self, packet);
            return;
        }

        pendingData.computeIfAbsent(packet.destinationIp, k -> new ArrayList<>()).add(packet);
        onSendRequest(self, packet.destinationIp);
    }

    private void forwardData(INetworkNode self, NetworkPacket packet) {
        var entry = routingTable.get(packet.destinationIp);
        if (entry == null || !entry.valid) return;

        entry.lifetime = simulator.getSimTime() + ACTIVE_ROUTE_TIMEOUT;
        simulator.sendUnicast(self, simulator.getNodeByIp(entry.nextHop), packet);
    }

    @Override
    public void tick(INetworkNode self) {
        long now = simulator.getSimTime();

        routingTable.values().removeIf(e -> !e.valid && e.lifetime < now);
        seenRreqs.entrySet().removeIf(e -> e.getValue() + PATH_DISCOVERY_TIME < now);
    }


    static class RouteEntry {
        public final IP destination;
        public IP nextHop;
        public int hopCount;
        public int seqNumber;
        public boolean valid;
        public boolean seqValid;
        public long lifetime;
        public Set<IP> precursors = new HashSet<>();

        public RouteEntry(IP destination) {
            this.destination = destination;
            valid = false;
        }
    }
}