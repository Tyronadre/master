package de.tyro.mcnetwork.routing.protocol;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.olsr.OLSRHelloMessage;
import de.tyro.mcnetwork.routing.packet.olsr.OLSRPacket;
import de.tyro.mcnetwork.routing.packet.olsr.OLSRTCMessage;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec2;
import org.antlr.v4.runtime.tree.Tree;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

public class OLSRProtocol implements IRoutingProtocol {
    private static final Logger logger = LogUtils.getLogger();

    //CONSTANTS
    private static final int HELLO_INTERVAL = 2_00;
    private static final int REFRESH_INTERVAL = 2_000;
    private static final int TC_INTERVAL = 5_00;
    private static final int DUP_HOLD_TIME = 30_000;
    private static final int NEIGHB_HOLD_TIME = 3 * REFRESH_INTERVAL;
    private static final int TOP_HOLD_TIME = 3 * TC_INTERVAL;
    private static final int MAX_JITTER = HELLO_INTERVAL / 4;
    private static final double C = 6.25; //ms


    private final INetworkNode node;
    private final SimulationEngine sim;

    // --- State ---


    private long nextTC;
    private long nextHello;
    private final Random rand = new Random();
    private int seqNum;
    private final List<Tuple<INetworkPacket, Integer>> pending = new ArrayList<>();

    public OLSRProtocol(INetworkNode node) {
        this.node = node;
        sim = SimulationEngine.getInstance(node.getLevel().isClientSide);
        var now = sim.getSimTime();

        nextHello = now + HELLO_INTERVAL - rand.nextInt(MAX_JITTER);
        nextTC = now + TC_INTERVAL - rand.nextInt(MAX_JITTER);
    }

    @Override
    public INetworkNode getNode() {
        return node;
    }

    private class SeenPackets {
        private final Map<Pair<IP, Integer>, SeenPacketEntry> entries = new HashMap<>();

        public boolean seen(OLSRPacket packet) {
            return entries.containsKey(getKey(packet));
        }

        private Pair<IP, Integer> getKey(OLSRPacket packet) {
            return new Pair<>(packet.getOriginatorIP(), packet.getSequenceNumber());
        }

        public SeenPacketEntry get(OLSRPacket packet) {
            return entries.get(getKey(packet));
        }

        public void put(OLSRPacket packet) {
            entries.put(getKey(packet), new SeenPacketEntry(sim.getSimTime() + DUP_HOLD_TIME));
        }

        public void simTick(long now) {
            entries.values().removeIf(it -> now >= it.expireTime);
        }

        private static class SeenPacketEntry {
            public SeenPacketEntry(long simTime) {
                expireTime = simTime;
                forwarded = false;
            }

            long expireTime;
            boolean forwarded;
        }
    }

    private final SeenPackets seenPackets = new SeenPackets();

    @Override
    public void onProtocolPacketReceived(IProtocolPaket pkt) {
        if (!(pkt instanceof OLSRPacket packet)) return;

        if (seenPackets.seen(packet)) {
            var entry = seenPackets.get(packet);
            if (entry.forwarded) return;
            entry.forwarded = true;
            defaultForwardingAlgorithm(packet);
            return;
        }

        seenPackets.put(packet);

        var message = packet.getMessageBlock();
        if (message instanceof OLSRHelloMessage hello) {
            processHelloMessages(packet, hello);
        } else if (message instanceof OLSRTCMessage tc) {
            processTCMessage(packet, tc);
            defaultForwardingAlgorithm(packet);
        }

    }

    private void defaultForwardingAlgorithm(IProtocolPaket packet) {
        var now = sim.getSimTime();
        var sender = packet.getNetworkFrame().getFrom().getIP();
        int ttl = packet.getNetworkFrame().getTtl();

        if (ttl <= 1) return;

        var link = linkSet.get(sender);
        if (link == null || link.symTime < now) return;

        // Only forward if sender selected me as MPR
        if (!mprSelectorSet.contains(sender)) return;

        sim.broadcast(node, packet, ttl - 1);
    }

    private void computeMPRs() {
        // 8.3.1.  MPR Computation
        //
        //   The following specifies a proposed heuristic for selection of MPRs.
        //   It constructs an MPR-set that enables a node to reach any node in the
        //   symmetrical strict 2-hop neighborhood through relaying by one MPR
        //   node with willingness different from WILL_NEVER.  The heuristic MUST
        //   be applied per interface, I.  The MPR set for a node is the union of
        //   the MPR sets found for each interface.  The following terminology
        //   will be used in describing the heuristics:
        //
        //       neighbor of an interface
        //
        //              a node is a "neighbor of an interface" if the interface
        //              (on the local node) has a link to any one interface of
        //              the neighbor node.
        //
        //       2-hop neighbors reachable from an interface
        //
        //              the list of 2-hop neighbors of the node that can be
        //              reached from neighbors of this interface.
        //
        //       MPR set of an interface
        //
        //              a (sub)set of the neighbors of an interface with a
        //              willingness different from WILL_NEVER, selected such that
        //              through these selected nodes, all strict 2-hop neighbors
        //              reachable from that interface are reachable.
        //
        //       N:
        //              N is the subset of neighbors of the node, which are
        //              neighbor of the interface I.
        //
        //       N2:
        //              The set of 2-hop neighbors reachable from the interface
        //              I, excluding:
        //
        //               (i)   the nodes only reachable by members of N with
        //                     willingness WILL_NEVER
        //
        //               (ii)  the node performing the computation
        //
        //               (iii) all the symmetric neighbors: the nodes for which
        //                     there exists a symmetric link to this node on some
        //                     interface.

        mprSet.clear();
        Set<NeighborSet.NeighborSetEntry> N = neighborSet.getAll();
        Set<TwoHopNeighborSet.TwoHopNeighborSetEntry> N2 = twoHopNeighborSet.getN2Set();

        //    D(y):
        //              The degree of a 1-hop neighbor node y (where y is a
        //              member of N), is defined as the number of symmetric
        //              neighbors of node y, EXCLUDING all the members of N and
        //              EXCLUDING the node performing the computation.

        Function<NeighborSet.NeighborSetEntry, Integer> dY = neighborSetEntry -> {
            var count = 0;
            for (var twoHopNeighborEntry : twoHopNeighborSet.entries) {
                if (!twoHopNeighborEntry.neighborAddress.equals(neighborSetEntry.address)) continue;
                if (twoHopNeighborEntry.twoHopAddress.equals(node.getIP())) continue;
                if (N.stream().anyMatch(it -> it.address.equals(twoHopNeighborEntry.twoHopAddress))) continue;

                count++;
            }

            return count;
        };


        //
        //   The proposed heuristic is as follows:
        //
        //     1    Start with an MPR set made of all members of N with
        //          N_willingness equal to WILL_ALWAYS
        mprSet.addAll(N.stream().filter(it -> it.willingness == Willingness.WILL_ALWAYS).map(it -> it.address).toList());
        //
        //     2    Calculate D(y), where y is a member of N, for all nodes in N.
        Map<IP, Integer> dYMap = new HashMap<>();
        for (var n : N) dYMap.put(n.address, dY.apply(n));

        //     3    Add to the MPR set those nodes in N, which are the *only*
        //          nodes to provide reachability to a node in N2.  For example,
        //          if node b in N2 can be reached only through a symmetric link
        //          to node a in N, then add node a to the MPR set.  Remove the
        //          nodes from N2 which are now covered by a node in the MPR set.

        Set<IP> n2NodesToSkip = new HashSet<>();
        for (TwoHopNeighborSet.TwoHopNeighborSetEntry twoHopNeighborEntry : N2) {
            if (n2NodesToSkip.contains(twoHopNeighborEntry.twoHopAddress)) continue;

            var paths = dYMap.get(twoHopNeighborEntry.neighborAddress);
            if (paths == 1) {
                mprSet.add(twoHopNeighborEntry.neighborAddress);
                n2NodesToSkip.add(twoHopNeighborEntry.twoHopAddress);
            }
        }

        N2.removeIf(it -> n2NodesToSkip.contains(it.twoHopAddress));

        //     4    While there exist nodes in N2 which are not covered by at
        //          least one node in the MPR set:
        while (!N2.isEmpty()) {
            //          4.1  For each node in N, calculate the reachability, i.e., the
            //               number of nodes in N2 which are not yet covered by at
            //               least one node in the MPR set, and which are reachable
            //               through this 1-hop neighbor;

            Map<NeighborSet.NeighborSetEntry, Integer> reachability = new HashMap<>();
            for (var n : N) {
                var reachable = (int) N2.stream().filter(it -> it.neighborAddress.equals(n.address)).count();
                if (reachable == 0) continue;
                reachability.put(n, reachable);
            }
            //          4.2  Select as a MPR the node with highest N_willingness among
            //               the nodes in N with non-zero reachability.  In case of
            //               multiple choice select the node which provides
            //               reachability to the maximum number of nodes in N2.  In
            //               case of multiple nodes providing the same amount of
            //               reachability, select the node as MPR whose D(y) is
            //               greater.  Remove the nodes from N2 which are now covered
            //               by a node in the MPR set.

            var sorted = reachability.keySet().stream().sorted((o1, o2) -> {
                if (o1.willingness != o2.willingness) return o1.willingness.ordinal() - o2.willingness.ordinal();
                if (!Objects.equals(reachability.get(o1), reachability.get(o2))) return reachability.get(o1) - reachability.get(o2);
                return dYMap.get(o1.address) - dYMap.get(o2.address);
            });
            var nextMPR = sorted.findFirst().orElse(null);
            if (nextMPR == null) {
                logger.warn("Next MPR is null");
                break;
            }

            mprSet.add(nextMPR.address);
            N2.removeIf(it -> it.neighborAddress.equals(nextMPR.address));

        }
        //     5    A node's MPR set is generated from the union of the MPR sets
        //          for each interface.  As an optimization, process each node, y,
        //          in the MPR set in increasing order of N_willingness.  If all
        //          nodes in N2 are still covered by at least one node in the MPR
        //          set excluding node y, and if N_willingness of node y is
        //          smaller than WILL_ALWAYS, then node y MAY be removed from the
        //          MPR set.
        //
        //   Other algorithms, as well as improvements over this algorithm, are
        //   possible.  For example, assume that in a multiple-interface scenario
        //   there exists more than one link between nodes 'a' and 'b'.  If node
        //   'a' has selected node 'b' as MPR for one of its interfaces, then node
        //   'b' can be selected as MPR without additional performance loss by any
        //   other interfaces on node 'a'.
    }

    @Override
    public void send(INetworkPacket packet, int ttl) {
        if (hasRoute(packet.getDestinationIP())) {
            sim.unicast(node, sim.getNode(routingTable.get(packet.getDestinationIP()).nextAddress), packet, ttl);
        } else {
            pending.add(new Tuple<>(packet, ttl));
        }
    }

    @Override
    public void discoverRoute(IP destinationIp) {
        // OLSR ist proaktiv
    }

    @Override
    public boolean hasRoute(IP destination) {
        return routingTable.containsKey(destination);
    }

    @Override
    public void simTick() {
        var now = sim.getSimTime();

        seenPackets.simTick(now);

        if (!node.getLevel().isClientSide()) {
            if (now >= nextHello) {
                generateHelloMessage();
                nextHello = now + HELLO_INTERVAL - rand.nextInt(MAX_JITTER);
            }

            if (now >= nextTC) {
                generateTCMessage();
                nextTC = now + TC_INTERVAL - rand.nextInt(MAX_JITTER);
            }
        }

        detectNeighborhoodChanges(now);
        topologySet.removeIf(e -> e.time < now);
        mprSelectorSet.entries.entrySet().removeIf(e -> e.getValue().time < now);

        for (Iterator<Tuple<INetworkPacket, Integer>> iterator = pending.iterator(); iterator.hasNext(); ) {
            Tuple<INetworkPacket, Integer> pend = iterator.next();
            if (hasRoute(pend.getA().getDestinationIP())) {
                send(pend.getA(), pend.getB());
            }
            iterator.remove();
        }
    }

    private void detectNeighborhoodChanges(long now) {
        // 8.5.  Neighborhood and 2-hop Neighborhood Changes
        //
        //   A change in the neighborhood is detected when:
        //
        //     -    The L_SYM_time field of a link tuple expires.  This is
        //          considered as a neighbor loss if the link described by the
        //          expired tuple was the last link with a neighbor node (on the
        //          contrary, a link with an interface may break while a link with
        //          another interface of the neighbor node remains without being
        //          observed as a neighborhood change).
        //
        //     -    A new link tuple is inserted in the Link Set with a non
        //          expired L_SYM_time or a tuple with expired L_SYM_time is
        //          modified so that L_SYM_time becomes non-expired.  This is
        //          considered as a neighbor appearance if there was previously no
        //          link tuple describing a link with the corresponding neighbor
        //          node.
        //
        //   A change in the 2-hop neighborhood is detected when a 2-hop neighbor
        //   tuple expires or is deleted according to section 8.2.
        //
        //   The following processing occurs when changes in the neighborhood or
        //   the 2-hop neighborhood are detected:
        //
        //     -    In case of neighbor loss, all 2-hop tuples with
        //          N_neighbor_main_addr == Main Address of the neighbor MUST be
        //          deleted.
        //
        //     -    In case of neighbor loss, all MPR selector tuples with
        //          MS_main_addr == Main Address of the neighbor MUST be deleted
        //
        //     -    The MPR set MUST be re-calculated when a neighbor appearance
        //          or loss is detected, or when a change in the 2-hop
        //          neighborhood is detected.
        //
        //     -    An additional HELLO message MAY be sent when the MPR set
        //          changes.

        boolean neighborhoodChanged = false;
        boolean twoHopChanged = false;

        // Remove expired link tuples
        Set<IP> lostNeighbors = new HashSet<>();
        for (Iterator<Map.Entry<IP, LinkSet.LinkSetEntry>> iterator = linkSet.entries.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            var link = entry.getValue();
            if (link.time < now) {
                lostNeighbors.add(entry.getKey());
                iterator.remove();
            }
        }

        // Process neighbor losses
        for (IP neighbor : lostNeighbors) {
            // Remove from neighborSet if exists
            neighborSet.entries.remove(neighbor);
            // Delete 2-hop tuples
            twoHopNeighborSet.removeIf(e -> e.neighborAddress.equals(neighbor));
            // Delete MPR selector tuples
            mprSelectorSet.entries.remove(neighbor);
            neighborhoodChanged = true;
        }

        // Remove expired 2-hop tuples
        int before = twoHopNeighborSet.entries.size();
        twoHopNeighborSet.removeIf(e -> e.time < now);
        if (twoHopNeighborSet.entries.size() < before) {
            twoHopChanged = true;
        }

        // For neighbor appearances, they are detected in processHelloMessageNeighborSensing
        // when a new link is added with symTime >= now and no previous neighbor

        // Re-calculate MPR set if changes
        if (neighborhoodChanged || twoHopChanged) {
            computeMPRs();
            // Optionally send additional HELLO, but MAY, so skip for now
        }
    }


    @Override
    public void render(RenderUtil renderUtil) {
        var pose = renderUtil.getPoseStack();
        var font = renderUtil.getFont();
        pose.pushPose();
        var width = getRenderSize(renderUtil.getFont()).x;
        var y = 0;

        pose.scale(0.5f, 0.5f, 0.5f);
        width *= 2;

        var symNeighbors = neighborSet.getAll().stream().filter(it -> it.isSym).count();
        var asymNeighbors = neighborSet.entries.size() - symNeighbors;
        var symNeighborsString = String.valueOf(symNeighbors);
        var asymNeighborString = String.valueOf(asymNeighbors);

        renderUtil.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "Neighbors (asym | sym)", width, y);
        renderUtil.drawString(asymNeighborString, RenderUtil.Color.MAGENTA.value, width / 2 - (5 + font.width(asymNeighborString) + font.width(symNeighborsString)), y, false);
        renderUtil.drawString(symNeighborsString, RenderUtil.Color.GREEN.value, width / 2 - (font.width(symNeighborsString)), y, false);
        y += 10;

        var symLinks = linkSet.getAll().stream().filter(it -> it.symTime > sim.getSimTime()).count();
        var asymLinks = linkSet.getAll().stream().filter(it -> it.asymTime > sim.getSimTime()).count() - symLinks;
        var brokenLinks = linkSet.getAll().size() - symLinks - asymLinks;

        var symLinksString = String.valueOf(symLinks);
        var asymLinksString = String.valueOf(asymLinks);
        var brokenLinksString = String.valueOf(brokenLinks);

        renderUtil.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "Links (broken | asym | sym)", width, y);
        renderUtil.drawString(brokenLinksString, RenderUtil.Color.RED.value, width / 2 - (10 + font.width(brokenLinksString) + font.width(asymLinksString) + font.width(symLinksString)), y, false);
        renderUtil.drawString(asymLinksString, RenderUtil.Color.MAGENTA.value, width / 2 - (5 + font.width(asymLinksString) + font.width(symLinksString)), y, false);
        renderUtil.drawString(symLinksString, RenderUtil.Color.GREEN.value, width / 2 - (font.width(symLinksString)), y, false);
        y += 10;

        renderUtil.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "MPRSet", width, y);
        renderUtil.drawCollectionAsString(RenderUtil.Align.RIGHT, mprSet, (int) (width - font.width("MPRSet ")), renderUtil.getTextColorFromAlpha(), width, y);
        y += 10;

        renderUtil.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "MPRSelectorSet", width, y);
        renderUtil.drawCollectionAsString(RenderUtil.Align.RIGHT, mprSelectorSet.entries.keySet(), (int) (width - font.width("MPRSelectorSet ")), renderUtil.getTextColorFromAlpha(), width, y);
        y += 10;


        renderUtil.renderHLineWithAlphaColor(width, y);
        y += 5;

        renderUtil.drawString(RenderUtil.Align.LEFT, "Routing Tree", RenderUtil.Color.WHITE.value, width, y);
        y += 15;

        renderRoutingTable(renderUtil, font, width, y);

        pose.popPose();

    }

    private Map<IP, List<IP>> buildChildrenMap() {
        Map<IP, List<IP>> map = new HashMap<>();

        for (var entry : routingTable.values()) {

            if (entry.destinationAddress.equals(entry.nextAddress)) continue;

            map.computeIfAbsent(entry.nextAddress, k -> new ArrayList<>()).add(entry.destinationAddress);
        }

        return map;
    }

    private int computeSubtreeSize(IP root, Map<IP, List<IP>> childrenMap) {

        var children = childrenMap.getOrDefault(root, List.of());

        if (children.isEmpty()) return 1;

        int sum = 0;
        for (var child : children) {
            sum += computeSubtreeSize(child, childrenMap);
        }

        return Math.max(1, sum);
    }

    private void renderSubtree(RenderUtil renderUtil, IP parent, int depth, int parentX, int startY, Map<IP, List<IP>> childrenMap, Map<IP, Integer> subtreeSizes, Map<IP, Integer> nodeY, int colWidth, int rowHeight) {

        var children = childrenMap.getOrDefault(parent, List.of());
        if (children.isEmpty()) return;

        int x = parentX + 50;
        int currentY = startY;

        int parentY = nodeY.get(parent);

        int trunkX = parentX + 30;

        int firstChildY = -1;
        int lastChildY = -1;

        for (var child : children) {

            int subtreeHeight = subtreeSizes.getOrDefault(child, 1) * rowHeight;

            int childY = currentY + subtreeHeight / 2;

            renderUtil.drawString(child.toString(), RenderUtil.Color.WHITE.value, x, childY, false);

            nodeY.put(child, childY);

            if (firstChildY == -1) firstChildY = childY;
            lastChildY = childY;

            // horizontal from trunk to child
            renderUtil.drawLine(trunkX, childY + 4, x - 5, childY + 4, RenderUtil.Color.WHITE.value);

            renderSubtree(renderUtil, child, depth + 1, x + 20, currentY, childrenMap, subtreeSizes, nodeY, colWidth, rowHeight);

            currentY += subtreeHeight;
        }

        renderUtil.drawLine(trunkX, firstChildY + 5, trunkX, lastChildY + 5, RenderUtil.Color.WHITE.value);
        renderUtil.drawLine(parentX + 20, parentY + 5, trunkX, parentY + 5, RenderUtil.Color.WHITE.value);

    }

    private void renderRoutingTable(RenderUtil renderUtil, Font font, float width, int y) {
        // layout constants
        int colWidth = font.width(IP.BROADCAST.toString());
        int rowHeight = 10;
        int startX = (int) (-width / 2);

        var childrenMap = buildChildrenMap();

        renderUtil.drawString(node.getIP().toString(), RenderUtil.Color.GREEN.value, startX, y, false);

        var d1Nodes = routingTable.values().stream().filter(e -> e.distance == 1).map(e -> e.destinationAddress).toList();

        Map<IP, Integer> subtreeSizes = new HashMap<>();
        for (var n : d1Nodes) {
            subtreeSizes.put(n, computeSubtreeSize(n, childrenMap));
        }

        Map<IP, Integer> nodeY = new HashMap<>();

        int currentY = y;

        for (var d1 : d1Nodes) {

            int subtreeHeight = subtreeSizes.get(d1) * rowHeight;

            // center d1 node in its subtree block
            int d1Y = currentY + subtreeHeight / 2;

            int d1X = startX + colWidth - 20;

            renderUtil.drawString(d1.toString(), RenderUtil.Color.WHITE.value, d1X, d1Y, false);

            nodeY.put(d1, d1Y);

            // render subtree recursively
            renderSubtree(renderUtil, d1, 2, d1X + 20, currentY, childrenMap, subtreeSizes, nodeY, colWidth, rowHeight);

            int trunkX = startX + 10;

            renderUtil.drawLine(trunkX, y + 10, trunkX, currentY + subtreeHeight - (float) rowHeight / 2, RenderUtil.Color.WHITE.value, 2);

            // horizontal into d1 node
            renderUtil.drawLine(trunkX, d1Y + 4, d1X - 5, d1Y + 4, RenderUtil.Color.WHITE.value);

            currentY += subtreeHeight + 5;
        }
    }

    @Override
    public Vec2 getRenderSize(Font font) {
        int rowHeight = 6;

        var childrenMap = buildChildrenMap();
        Map<IP, Integer> subtreeSizes = new HashMap<>();
        var d1Nodes = routingTable.values().stream().filter(e -> e.distance == 1).map(e -> e.destinationAddress).toList();
        for (var n : d1Nodes) {
            subtreeSizes.put(n, computeSubtreeSize(n, childrenMap));
        }

        var height = 50;
        for (var d1 : d1Nodes) {
            height += subtreeSizes.get(d1) * rowHeight;
        }


        return new Vec2(200, height);
    }


    private static class RoutingTable extends HashMap<IP, RoutingTable.RoutingTableEntry> {
        private record RoutingTableEntry(IP destinationAddress, IP nextAddress, Short distance) {
        }
    }

    private final RoutingTable routingTable = new RoutingTable();

    private void computeRoutingTable() {
        var now = sim.getSimTime();
        //   To construct the routing table of node X, a shortest path algorithm
        //   is run on the directed graph containing the arcs X -> Y where Y is
        //   any symmetric neighbor of X (with Neighbor Type equal to SYM), the
        //   arcs Y -> Z where Y is a neighbor node with willingness different of
        //   WILL_NEVER and there exists an entry in the 2-hop Neighbor set with Y
        //   as N_neighbor_main_addr and Z as N_2hop_addr, and the arcs U -> V,
        //   where there exists an entry in the topology set with V as T_dest_addr
        //   and U as T_last_addr.
        //
        //   The following procedure is given as an example to calculate (or
        //   recalculate) the routing table:
        //
        //     1    All the entries from the routing table are removed.
        routingTable.clear();
        //
        //     2    The new routing entries are added starting with the
        //          symmetric neighbors (h=1) as the destination nodes. Thus, for
        //          each neighbor tuple in the neighbor set where:
        //
        //               N_status   = SYM
        for (var neighbor : neighborSet.getAll()) {
            if (!neighbor.isSym) continue;
            if (linkSet.get(neighbor.address).time < now) continue;
            //          (there is a symmetric link to the neighbor), and for each
            //          associated link tuple of the neighbor node such that L_time >=
            //          current time, a new routing entry is recorded in the routing
            //          table with:
            //
            //               R_dest_addr  = L_neighbor_iface_addr, of the
            //                              associated link tuple;
            //
            //               R_next_addr  = L_neighbor_iface_addr, of the
            //                              associated link tuple;
            //
            //               R_dist       = 1;
            //
            //               R_iface_addr = L_local_iface_addr of the
            //                              associated link tuple.
            //
            routingTable.put(neighbor.address, new RoutingTable.RoutingTableEntry(neighbor.address, neighbor.address, (short) 1));

            //          If in the above, no R_dest_addr is equal to the main address
            //          of the neighbor, then another new routing entry with MUST be
            //          added, with:
            //
            //               R_dest_addr  = main address of the neighbor;
            //
            //               R_next_addr  = L_neighbor_iface_addr of one of the
            //                              associated link tuple with L_time >=
            //               current time;
            //
            //               R_dist       = 1;
            //
            //               R_iface_addr = L_local_iface_addr of the
            //                              associated link tuple.
            //
            //this will not happen, cause we dont have multiple interfaces
        }
        //     3    for each node in N2, i.e., a 2-hop neighbor which is not a
        //          neighbor node or the node itself, and such that there exist at
        //          least one entry in the 2-hop neighbor set where
        //          N_neighbor_main_addr correspond to a neighbor node with
        //          willingness different of WILL_NEVER, one selects one 2-hop
        //          tuple and creates one entry in the routing table with:
        //
        for (var entry : twoHopNeighborSet.getN2Set()) {
            routingTable.put(entry.twoHopAddress, new RoutingTable.RoutingTableEntry(entry.twoHopAddress, entry.neighborAddress, (short) 2));
        }
        //               R_dest_addr  =  the main address of the 2-hop neighbor;
        //
        //               R_next_addr  = the R_next_addr of the entry in the
        //                              routing table with:
        //
        //                                  R_dest_addr == N_neighbor_main_addr
        //                                                 of the 2-hop tuple;
        //
        //               R_dist       = 2;
        //
        //               R_iface_addr = the R_iface_addr of the entry in the
        //                              routing table with:
        //
        //                                  R_dest_addr == N_neighbor_main_addr
        //                                                 of the 2-hop tuple;
        //
        //
        //     3    The new route entries for the destination nodes h+1 hops away
        //          are recorded in the routing table.  The following procedure
        //          MUST be executed for each value of h, starting with h=2 and
        //          incrementing it by 1 each time.  The execution will stop if no
        //          new entry is recorded in an iteration.
        short h = 2;
        boolean entryAdded;
        do {
            entryAdded = false;
            //          3.1  For each topology entry in the topology table, if its
            //               T_dest_addr does not correspond to R_dest_addr of any
            //               route entry in the routing table AND its T_last_addr
            //               corresponds to R_dest_addr of a route entry whose R_dist
            //               is equal to h, then a new route entry MUST be recorded in
            //               the routing table (if it does not already exist) where:
            for (var topologySetEntry : topologySet.entries) {
                if (routingTable.containsKey(topologySetEntry.address)) continue;

                var routeVia = routingTable.get(topologySetEntry.lastAddress);
                if (routeVia == null) continue;
                if (routeVia.distance != h) continue;

                var newRoutingEntry = new RoutingTable.RoutingTableEntry(topologySetEntry.address, routeVia.nextAddress, (short) (h + 1));
                routingTable.put(topologySetEntry.address, newRoutingEntry);
                entryAdded = true;
            }


            //                    R_dest_addr  = T_dest_addr;
            //
            //                    R_next_addr  = R_next_addr of the recorded
            //                                   route entry where:
            //
            //                                   R_dest_addr == T_last_addr
            //
            //                    R_dist       = h+1; and
            //
            //                    R_iface_addr = R_iface_addr of the recorded
            //                                   route entry where:
            //
            //                                      R_dest_addr == T_last_addr.
            //
            //          3.2  Several topology entries may be used to select a next hop
            //               R_next_addr for reaching the node R_dest_addr.  When h=1,
            //               ties should be broken such that nodes with highest
            //               willingness and MPR selectors are preferred as next hop.
            //

            h++;

            if (h == Short.MAX_VALUE) {
                logger.warn("FORCE QUITTING ROUTING TABLE COMPUTATION");
                break;
            }
        } while (entryAdded);
    }

    private void processTCMessage(OLSRPacket packet, OLSRTCMessage tcMessage) {
        var now = sim.getSimTime();
        // 9.5.  TC Message Processing
        //
        //   Upon receiving a TC message, the "validity time" MUST be computed
        //   from the Vtime field of the message header (see section 3.3.2).  The
        //   topology set SHOULD then be updated as follows (using section 19 for
        //   comparison of ANSN):

        var validityTime = computeValidityTime(packet.getVTime());
        var sender = packet.getNetworkFrame().getFrom().getIP();

        //     1    If the sender interface (NB: not originator) of this message
        //          is not in the symmetric 1-hop neighborhood of this node, the
        //          message MUST be discarded.
        var link = linkSet.get(sender);
        if (link == null || link.symTime < now) return;
        //
        //     2    If there exist some tuple in the topology set where:
        //
        //               T_last_addr == originator address AND
        //
        //               T_seq       >  ANSN,
        //
        //          then further processing of this TC message MUST NOT be
        //          performed and the message MUST be silently discarded (case:
        //          message received out of order).
        //

        if (topologySet.getSetForLastAddress(packet.getOriginatorIP()).stream().anyMatch(it -> it.seqNumber > tcMessage.advertisedNeighborSequenceNumber())) return;

        //     3    All tuples in the topology set where:
        //
        //               T_last_addr == originator address AND
        //
        //               T_seq       <  ANSN
        //
        //          MUST be removed from the topology set.
        topologySet.removeIf(it -> it.lastAddress.equals(packet.getOriginatorIP()) && it.seqNumber < tcMessage.advertisedNeighborSequenceNumber());

        //     4    For each of the advertised neighbor main address received in
        //          the TC message:
        for (var newNeighborIP : tcMessage.advertisedNeighborMainAddresses()) {
            //
            //          4.1  If there exist some tuple in the topology set where:
            //
            //                    T_dest_addr == advertised neighbor main address, AND
            //
            //                    T_last_addr == originator address,
            //
            //               then the holding time of that tuple MUST be set to:
            //
            //                    T_time      =  current time + validity time.

            var filtered = topologySet.filteredList(it -> it.address.equals(newNeighborIP) && it.lastAddress.equals(packet.getOriginatorIP()));
            if (!filtered.isEmpty()) filtered.forEach(it -> it.time = now + validityTime);
                //
                //          4.2  Otherwise, a new tuple MUST be recorded in the topology
                //               set where:
                //
                //                    T_dest_addr = advertised neighbor main address,
                //
                //                    T_last_addr = originator address,
                //
                //                    T_seq       = ANSN,
                //
                //                    T_time      = current time + validity time.
            else {
                topologySet.add(new TopologySet.TopologySetEntry(newNeighborIP, packet.getOriginatorIP(), tcMessage.advertisedNeighborSequenceNumber(), now + validityTime));
            }
        }

        // this only has to be called when the routing table gets an update in theory
        computeRoutingTable();
    }


    private int advertisedNeighborSequenceNumber = 0;
    private Set<IP> advertisedNeighborMainAddresses = new HashSet<>();

    private void generateTCMessage() {
        // 9.2.  Advertised Neighbor Set
        //
        //   A TC message is sent by a node in the network to declare a set of
        //   links, called advertised link set which MUST include at least the
        //   links to all nodes of its MPR Selector set, i.e., the neighbors which
        //   have selected the sender node as a MPR.
        //
        //   If, for some reason, it is required to distribute redundant TC
        //   information, refer to section 15.
        //
        //   The sequence number (ANSN) associated with the advertised neighbor
        //   set is also sent with the list.  The ANSN number MUST be incremented
        //   when links are removed from the advertised neighbor set; the ANSN
        //   number SHOULD be incremented when links are added to the advertised
        //   neighbor set.
        //
        // 9.3.  TC Message Generation
        //
        //   In order to build the topology information base, each node, which has
        //   been selected as MPR, broadcasts Topology Control (TC) messages.  TC
        //   messages are flooded to all nodes in the network and take advantage
        //   of MPRs.  MPRs enable a better scalability in the distribution of
        //   topology information [1].
        //
        //   The list of addresses can be partial in each TC message (e.g., due to
        //   message size limitations, imposed by the network), but parsing of all
        //   TC messages describing the advertised link set of a node MUST be
        //   complete within a certain refreshing period (TC_INTERVAL).  The
        //   information diffused in the network by these TC messages will help
        //   each node calculate its routing table.
        //
        //   When the advertised link set of a node becomes empty, this node
        //   SHOULD still send (empty) TC-messages during the a duration equal to
        //   the "validity time" (typically, this will be equal to TOP_HOLD_TIME)
        //   of its previously emitted TC-messages, in order to invalidate the
        //   previous TC-messages.  It SHOULD then stop sending TC-messages until
        //   some node is inserted in its advertised link set.
        //
        //   A node MAY transmit additional TC-messages to increase its
        //   reactiveness to link failures.  When a change to the MPR selector set
        //   is detected and this change can be attributed to a link failure, a
        //   TC-message SHOULD be transmitted after an interval shorter than
        //   TC_INTERVAL.

        var newAdvertisedNeighborMainAddresses = mprSelectorSet.entries.keySet();
        if (!newAdvertisedNeighborMainAddresses.equals(advertisedNeighborMainAddresses)) {
            advertisedNeighborSequenceNumber++;
            advertisedNeighborMainAddresses = newAdvertisedNeighborMainAddresses;
        }

        if (node.getIP().equals(new IP("0.0.0.10")))
            sim.setSimSpeed(0.01);

        sim.broadcast(node, new OLSRPacket(node.getIP(), OLSRPacket.Type.TC, TOP_HOLD_TIME, seqNum++, new OLSRTCMessage(advertisedNeighborSequenceNumber, newAdvertisedNeighborMainAddresses)), 255);
    }

    private void processHelloMessages(OLSRPacket packet, OLSRHelloMessage helloMessage) {
        // 6.4.  HELLO Message Processing
        //
        //   A node processes incoming HELLO messages for the purpose of
        //   conducting link sensing (detailed in section 7), neighbor detection
        //   and MPR selector set population (detailed in section 8)
        //
        // 7.  Link Sensing
        //
        //   Link sensing populates the local link information base.  Link sensing
        //   is exclusively concerned with OLSR interface addresses and the
        //   ability to exchange packets between such OLSR interfaces.
        //
        //   The mechanism for link sensing is the periodic exchange of HELLO
        //   messages.

        processHelloMessageLinkSensing(packet, helloMessage);

        // 8.  Neighbor Detection
        //
        //   Neighbor detection populates the neighborhood information base and
        //   concerns itself with nodes and node main addresses.  The relationship
        //   between OLSR interface addresses and main addresses is described in
        //   section 5.
        //
        //   The mechanism for neighbor detection is the periodic exchange of
        //   HELLO messages.
        //

        helloMessage.entries().forEach(it -> processHelloMessageNeighbor(packet, it));

        // 8.2.  Populating the 2-hop Neighbor Set
        //
        //   The 2-hop neighbor set describes the set of nodes which have a
        //   symmetric link to a symmetric neighbor.  This information set is
        //   maintained through periodic exchange of HELLO messages as described
        //   in this section.

        helloMessage.entries().forEach(it -> processHelloMessage2HopNeighbor(packet, it));

        // 8.3.  Populating the MPR set
        //
        //   MPRs are used to flood control messages from a node into the network
        //   while reducing the number of retransmissions that will occur in a
        //   region.  Thus, the concept of MPR is an optimization of a classical
        //   flooding mechanism.
        //
        //   Each node in the network selects, independently, its own set of MPRs
        //   among its symmetric 1-hop neighborhood.  The symmetric links with
        //   MPRs are advertised with Link Type MPR_NEIGH instead of SYM_NEIGH in
        //   HELLO messages.
        //
        //   The MPR set MUST be calculated by a node in such a way that it,
        //   through the neighbors in the MPR-set, can reach all symmetric strict
        //   2-hop neighbors.  (Notice that a node, a, which is a direct neighbor
        //   of another node, b, is not also a strict 2-hop neighbor of node b).
        //   This means that the union of the symmetric 1-hop neighborhoods of the
        //   MPR nodes contains the symmetric strict 2-hop neighborhood.  MPR set
        //   recalculation should occur when changes are detected in the symmetric
        //   neighborhood or in the symmetric strict 2-hop neighborhood.
        //
        //   MPRs are computed per interface, the union of the MPR sets of each
        //   interface make up the MPR set for the node.
        //
        //   While it is not essential that the MPR set is minimal, it is
        //   essential that all strict 2-hop neighbors can be reached through the
        //   selected MPR nodes.  A node SHOULD select an MPR set such that any
        //   strict 2-hop neighbor is covered by at least one MPR node.  Keeping
        //   the MPR set small ensures that the overhead of the protocol is kept
        //   at a minimum.
        //
        //   The MPR set can coincide with the entire symmetric neighbor set.
        //   This could be the case at network initialization (and will correspond
        //   to classic link-state routing)

        computeMPRs();

        // 8.4.  Populating the MPR Selector Set
        //
        //   The MPR selector set of a node, n, is populated by the main addresses
        //   of the nodes which have selected n as MPR.  MPR selection is signaled
        //   through HELLO messages.

        helloMessage.entries().forEach(it -> processHelloMessageMPRSelector(packet, it));

        //this only has to be called when any of the above things changed in theory
        computeRoutingTable();
    }

    private void processHelloMessageMPRSelector(OLSRPacket packet, OLSRHelloMessage.OLSRHelloMessageEntry helloMessage) {
        var now = sim.getSimTime();
        // 8.4.1.  HELLO Message Processing
        //
        //   Upon receiving a HELLO message, if a node finds one of its own
        //   interface addresses in the list with a Neighbor Type equal to
        //   MPR_NEIGH, information from the HELLO message must be recorded in the
        //   MPR Selector Set.

        if (helloMessage.getNeighborType() != NeighborType.MPR_NEIGH) return;
        if (!helloMessage.getNeighborAddresses().contains(node.getIP())) return;

        //   The "validity time" MUST be computed from the Vtime field of the
        //   message header (see section 3.3.2).  The MPR Selector Set SHOULD then
        //   be updated as follows:

        var validityTime = computeValidityTime(packet.getVTime());

        //     1    If there exists no MPR selector tuple with:
        //
        //                    MS_main_addr   == Originator Address
        //
        //               then a new tuple is created with:
        //
        //                    MS_main_addr   =  Originator Address

        MPRSelectorSet.MPRSelectorSetEntry entry = mprSelectorSet.entries.computeIfAbsent(packet.getOriginatorIP(), k -> new MPRSelectorSet.MPRSelectorSetEntry(packet.getOriginatorIP(), 0L));

        //     2    The tuple (new or otherwise) with
        //
        //               MS_main_addr   == Originator Address
        //
        //          is then modified as follows:
        //
        //               MS_time        =  current time + validity time.

        entry.time = now + validityTime;

        //   Deletion of MPR selector tuples occurs in case of expiration of the
        //   timer or in case of link breakage as described in the "Neighborhood
        //   and 2-hop Neighborhood Changes".
    }

    private void processHelloMessage2HopNeighbor(OLSRPacket packet, OLSRHelloMessage.OLSRHelloMessageEntry helloMessage) {
        var now = sim.getSimTime();
        // 8.2.1.  HELLO Message Processing
        //
        //   The "Originator Address" of a HELLO message is the main neighborAddress of
        //   the node, which has emitted the message.
        //
        //   Upon receiving a HELLO message from a symmetric neighbor, a node
        //   SHOULD update its 2-hop Neighbor Set.  Notice, that a HELLO message
        //   MUST neither be forwarded nor be recorded in the duplicate set.
        //
        //   Upon receiving a HELLO message, the "validity time" MUST be computed
        //   from the Vtime field of the message header (see section 3.3.2).
        var validityTime = computeValidityTime(packet.getVTime());
        //
        //   If the Originator Address is the main neighborAddress of a
        //   L_neighbor_iface_addr from a link tuple included in the Link Set with
        //
        //          L_SYM_time >= current time (not expired)
        //
        //   (in other words: if the Originator Address is a symmetric neighbor)
        if (!linkSet.containsKey(packet.getOriginatorIP()) || linkSet.get(packet.getOriginatorIP()).symTime < now) return;

        //   then the 2-hop Neighbor Set SHOULD be updated as follows:
        //
        //     1    for each neighborAddress (henceforth: 2-hop neighbor neighborAddress), listed
        //          in the HELLO message with Neighbor Type equal to SYM_NEIGH or
        //          MPR_NEIGH:
        //
        if (helloMessage.getNeighborType() == NeighborType.SYM_NEIGH || helloMessage.getNeighborType() == NeighborType.MPR_NEIGH) {
            for (var address : helloMessage.getNeighborAddresses()) {
                //          1.1  if the main neighborAddress of the 2-hop neighbor neighborAddress = main
                //               neighborAddress of the receiving node:
                //
                //                    silently discard the 2-hop neighbor neighborAddress.
                //
                //               (in other words: a node is not its own 2-hop neighbor).
                if (address.equals(node.getIP())) continue;
                if (neighborSet.containsKey(address)) continue;
                //
                //          1.2  Otherwise, a 2-hop tuple is created with:
                //
                //                    N_neighbor_main_addr =  Originator Address;
                //
                //                    N_2hop_addr          =  main neighborAddress of the
                //                                            2-hop neighbor;
                //
                //                    N_time               =  current time
                //                                            + validity time.
                twoHopNeighborSet.add(new TwoHopNeighborSet.TwoHopNeighborSetEntry(packet.getOriginatorIP(), address, now + validityTime));
                //
                //               This tuple may replace an older similar tuple with same
                //               N_neighbor_main_addr and N_2hop_addr values.
            }
        }
        //     2    For each 2-hop node listed in the HELLO message with Neighbor
        //          Type equal to NOT_NEIGH, all 2-hop tuples where:
        //
        //               N_neighbor_main_addr == Originator Address AND
        //
        //               N_2hop_addr          == main neighborAddress of the
        //                                       2-hop neighbor
        //
        //          are deleted.
        if (helloMessage.getNeighborType() == NeighborType.NOT_NEIGH) {
            for (var address : helloMessage.getNeighborAddresses()) {
                twoHopNeighborSet.removeIf(v -> v.neighborAddress.equals(packet.getOriginatorIP()) && v.twoHopAddress.equals(address));
            }
        }
    }

    private void processHelloMessageNeighbor(OLSRPacket packet, OLSRHelloMessage.OLSRHelloMessageEntry messageEntry) {
        var now = sim.getSimTime();
        // 8.1.  Populating the Neighbor Set
        //
        //   A node maintains a set of neighbor tuples, based on the link tuples.
        //   This information is updated according to changes in the Link Set.
        //
        //   The Link Set keeps the information about the links, while the
        //   Neighbor Set keeps the information about the neighbors.  There is a
        //   clear association between those two sets, since a node is a neighbor
        //   of another node if and only if there is at least one link between the
        //   two nodes.
        //
        //   In any case, the formal correspondence between links and neighbors is
        //   defined as follows:
        //
        //          The "associated neighbor tuple" of a link tuple, is, if it
        //          exists, the neighbor tuple where:
        //
        //               N_neighbor_main_addr == main neighborAddress of
        //                                       L_neighbor_iface_addr
        //
        //          The "associated link tuples" of a neighbor tuple, are all the
        //          link tuples, where:
        //
        //               N_neighbor_main_addr == main neighborAddress of
        //                                       L_neighbor_iface_addr
        //
        //   The Neighbor Set MUST be populated by maintaining the proper
        //   correspondence between link tuples and associated neighbor tuples, as
        //   follows:
        //
        //     Creation
        //
        //          Each time a link appears, that is, each time a link tuple is
        //          created, the associated neighbor tuple MUST be created, if it
        //          doesn't already exist, with the following values:
        //
        //               N_neighbor_main_addr = main neighborAddress of
        //                                      L_neighbor_iface_addr
        //                                      (from the link tuple)
        //
        //          In any case, the N_status MUST then be computed as described
        //          in the next step
        //
        //     Update
        //
        //          Each time a link changes, that is, each time the information
        //          of a link tuple is modified, the node MUST ensure that the
        //          N_status of the associated neighbor tuple respects the
        //          property:
        //
        //               If the neighbor has any associated link tuple which
        //               indicates a symmetric link (i.e., with L_SYM_time >=
        //               current time), then
        //
        //                    N_status is set to SYM
        //
        //               else N_status is set to NOT_SYM
        //
        //     Removal
        //
        //          Each time a link is deleted, that is, each time a link tuple
        //          is removed, the associated neighbor tuple MUST be removed if
        //          it has no longer any associated link tuples.
        //
        //   These rules ensure that there is exactly one associated neighbor
        //   tuple for a link tuple, and that every neighbor tuple has at least
        //   one associated link tuple.
        //
        // 8.1.1.  HELLO Message Processing
        //
        //   The "Originator Address" of a HELLO message is the main neighborAddress of
        //   the node, which has emitted the message.  Likewise, the "willingness"
        //   MUST be computed from the Willingness field of the HELLO message (see
        //   section 6.1).
        //
        //   Upon receiving a HELLO message, a node SHOULD first update its Link
        //   Set as described before.  It SHOULD then update its Neighbor Set as
        //   follows:
        //
        //     -    if the Originator Address is the N_neighbor_main_addr from a
        //          neighbor tuple included in the Neighbor Set:
        //
        //               then, the neighbor tuple SHOULD be updated as follows:
        //
        //               N_willingness = willingness from the HELLO message

        for (var link : linkSet.getAll()) {
            {
                var neighbor = neighborSet.get(link.otherNode);
                neighbor.isSym = link.symTime >= now;
                neighbor.willingness = messageEntry.getWillingness();
            }
        }
    }

    private void processHelloMessageLinkSensing(OLSRPacket packet, OLSRHelloMessage helloMessage) {
        var now = sim.getSimTime();
        // 7.1.  Populating the Link Set
        //
        //   The Link Set is populated with information on links to neighbor
        //   nodes.  The process of populating this set is denoted "link sensing"
        //   and is performed using HELLO message exchange, updating a local link
        //   information base in each node.
        //
        //   Each node should detect the links between itself and neighbor nodes.
        //   Uncertainties over radio propagation may make some links
        //   unidirectional.  Consequently, all links MUST be checked in both
        //   directions in order to be considered valid.
        //
        //   A "link" is described by a pair of interfaces: a local and a remote
        //   interface.
        //
        //   For the purpose of link sensing, each neighbor node (more
        //   specifically, the link to each neighbor) has an associated isSym of
        //   either "symmetric" or "asymmetric".  "Symmetric" indicates, that the
        //   link to that neighbor node has been verified to be bi-directional,
        //   i.e., it is possible to transmit data in both directions.
        //   "Asymmetric" indicates that HELLO messages from the node have been
        //   heard (i.e., communication from the neighbor node is possible),
        //   however it is not confirmed that this node is also able to receive
        //   messages (i.e., communication to the neighbor node is not confirmed).
        //
        //   The information, acquired through and used by the link sensing, is
        //   accumulated in the link set.
        //
        // 7.1.1.  HELLO Message Processing
        //
        //   The "Originator Address" of a HELLO message is the main neighborAddress of
        //   the node, which has emitted the message.
        //
        //   Upon receiving a HELLO message, a node SHOULD update its Link Set.
        //   Notice, that a HELLO message MUST neither be forwarded nor be
        //   recorded in the duplicate set.
        //
        //   Upon receiving a HELLO message, the "validity time" MUST be computed
        //   from the Vtime field of the message header (see section 3.3.2).

        var validityTime = computeValidityTime(packet.getVTime());

        //   Then, the Link Set SHOULD be updated as follows:
        //
        //     1    Upon receiving a HELLO message, if there exists no link tuple
        //          with
        //
        //               L_neighbor_iface_addr == Source Address
        //
        //          a new tuple is created with
        //
        //               L_neighbor_iface_addr = Source Address
        //
        //               L_local_iface_addr    = Address of the interface
        //                                       which received the
        //                                       HELLO message
        //
        //               L_SYM_time            = current time - 1 (expired)
        //
        //               L_time                = current time + validity time
        var linkTuple = linkSet.computeIfAbsent(packet.getOriginatorIP(), k -> {
            neighborSet.put(k, new NeighborSet.NeighborSetEntry(k, false, Willingness.WILL_DEFAULT));
            return new LinkSet.LinkSetEntry(k, now - 1, 0, now + validityTime);
        });

        //     2    The tuple (existing or new) with:
        //
        //               L_neighbor_iface_addr == Source Address
        //
        //          is then modified as follows:
        //
        //          2.1  L_ASYM_time = current time + validity time;
        linkTuple.asymTime = now + validityTime;
        //
        //          2.2  if the node finds the neighborAddress of the interface which
        //               received the HELLO message among the addresses listed in
        //               the link message then the tuple is modified as follows:

        for (var messageEntry : helloMessage.entries()) {
            if (messageEntry.getNeighborAddresses().contains(node.getIP())) {
                //
                //               2.2.1
                //                    if Link Type is equal to LOST_LINK then
                //
                //                         L_SYM_time = current time - 1 (i.e., expired)
                if (messageEntry.getLinkType() == LinkType.LOST_LINK) {
                    linkTuple.symTime = now - 1;
                }
                //
                //               2.2.2
                //                    else if Link Type is equal to SYM_LINK or ASYM_LINK
                //                    then
                //
                //                         L_SYM_time = current time + validity time,
                //
                //                         L_time     = L_SYM_time + NEIGHB_HOLD_TIME
                else if (messageEntry.getLinkType() == LinkType.SYM_LINK || messageEntry.getLinkType() == LinkType.ASYM_LINK) {
                    linkTuple.symTime = now + validityTime;
                    linkTuple.time = now + validityTime + NEIGHB_HOLD_TIME;
                }
            }
            //
            //          2.3  L_time = max(L_time, L_ASYM_time)
            //
            linkTuple.time = Math.max(linkTuple.time, linkTuple.asymTime);

            //   The above rule for setting L_time is the following: a link losing its
            //   symmetry SHOULD still be advertised during at least the duration of
            //   the "validity time" advertised in the generated HELLO.  This allows
            //   neighbors to detect the link breakage.
        }
    }

    private long computeValidityTime(int vTime) {
        return vTime;
    }

    private void generateHelloMessage() {
        var now = sim.getSimTime();
        // This involves transmitting the Link Set, the Neighbor Set and the MPR
        // Set.  In principle, a HELLO message serves three independent tasks:
        //
        //   -    link sensing
        //
        //   -    neighbor detection
        //
        //   -    MPR selection signaling
        //
        // Three tasks are all are based on periodic information exchange within
        // a nodes neighborhood, and serve the common purpose of "local topology
        // discovery".  A HELLO message is therefore generated based on the
        // information stored in the Local Link Set, the Neighbor Set and the
        // MPR Set from the local link information base.
        //
        // A node must perform link sensing on each interface, in order to
        // detect links between the interface and neighbor interfaces.
        // Furthermore, a node must advertise its entire symmetric 1-hop
        // neighborhood on each interface in order to perform neighbor
        // detection.  Hence, for a given interface, a HELLO message will
        // contain a list of links on that interface (with associated link
        // types), as well as a list of the entire neighborhood (with an
        // associated neighbor types).
        // The Vtime field is set such that it corresponds to the value of the
        // node's NEIGHB_HOLD_TIME parameter.  The Htime field is set such that
        // it corresponds to the value of the node's HELLO_INTERVAL parameter
        // (see section 18.3).

        // The Willingness field is set such that it corresponds to the node's
        // willingness to forward traffic on behalf of other nodes (see section
        // 18.8).  A node MUST advertise the same willingness on all interfaces.

        Willingness willingness = Willingness.WILL_DEFAULT;

        Map<Pair<NeighborType, LinkType>, OLSRHelloMessage.OLSRHelloMessageEntry> grouped = new HashMap<>();
        HashSet<IP> coveredNeighbors = new HashSet<>();

        Function<LinkSet.LinkSetEntry, LinkType> linkTypeFunction = linkSetEntry -> {
            LinkType linkType;
            //   1    The Link Type set according to the following:
            //        1.1  if L_SYM_time >= current time (not expired)
            //                  Link Type = SYM_LINK
            if (linkSetEntry.symTime >= now) linkType = LinkType.SYM_LINK;
                //        1.2  Otherwise, if L_ASYM_time >= current time (not expired)
                //             AND L_SYM_time  <  current time (expired)
                //                  Link Type = ASYM_LINK
            else if (linkSetEntry.asymTime >= now) linkType = LinkType.ASYM_LINK;
                //        1.3  Otherwise, if L_ASYM_time < current time (expired) AND
                //                           L_SYM_time  < current time (expired)
                //                  Link Type = LOST_LINK
            else linkType = LinkType.LOST_LINK;
            return linkType;
        };

        Function<NeighborSet.NeighborSetEntry, NeighborType> neighborTypeFunction = neighborSetEntry -> {
            NeighborType neighborType = null;
            //        2.1  If the main neighborAddress, corresponding to
            //             L_neighbor_iface_addr, is included in the MPR set:
            //                  Neighbor Type = MPR_NEIGH
            if (mprSet.contains(neighborSetEntry.address)) neighborType = NeighborType.MPR_NEIGH;
                //        2.2  Otherwise, if the main neighborAddress, corresponding to
                //             L_neighbor_iface_addr, is included in the neighbor set:
                //             2.2.1
                //                  if N_status == SYM
                //                       Neighbor Type = SYM_NEIGH
            else if (neighborSetEntry.isSym) neighborType = NeighborType.SYM_NEIGH;
                //             2.2.2
                //                  Otherwise, if N_status == NOT_SYM
                //                       Neighbor Type = NOT_NEIGH
            else neighborType = NeighborType.NOT_NEIGH;

            return neighborType;
        };


        // The lists of addresses declared in a HELLO message is a list of
        // neighbor interface addresses computed as follows:
        for (var link : linkSet.getAll()) {
            // For each tuple in the Link Set, where L_local_iface_addr is the
            // interface where the HELLO is to be transmitted, and where L_time >=
            // current time (i.e., not expired), L_neighbor_iface_addr is advertised
            // with:

            LinkType linkType = linkTypeFunction.apply(link);
            NeighborType neighborType = neighborTypeFunction.apply(neighborSet.get(link.otherNode));

            grouped.computeIfAbsent(new Pair<>(neighborType, linkType), k -> new OLSRHelloMessage.OLSRHelloMessageEntry(willingness, k.getFirst(), k.getSecond())).addNeighbor(link.otherNode);
            coveredNeighbors.add(link.otherNode);
        }


        for (var neighbor : neighborSet.getAll()) {
            if (coveredNeighbors.contains(neighbor.address)) continue;

            NeighborType neighborType = neighborTypeFunction.apply(neighbor);

            grouped.computeIfAbsent(new Pair<>(neighborType, LinkType.UNSPEC_LINK), k -> new OLSRHelloMessage.OLSRHelloMessageEntry(willingness, k.getFirst(), k.getSecond())).addNeighbor(neighbor.address);
        }

        // For a node with a single OLSR interface, the main neighborAddress is simply
        // the neighborAddress of the OLSR interface, i.e., for a node with a single
        // OLSR interface the main neighborAddress, corresponding to
        // L_neighbor_iface_addr is simply L_neighbor_iface_addr.
        //
        // A HELLO message can be partial (e.g., due to message size
        // limitations, imposed by the network), the rule being the following,
        // on each interface: each link and each neighbor node MUST be cited at
        // least once within a predetermined refreshing period,
        // REFRESH_INTERVAL.  To keep track of fast connectivity changes, a
        // HELLO message must be sent at least every HELLO_INTERVAL period,
        // smaller than or equal to REFRESH_INTERVAL.
        //
        // Notice that for limiting the impact from loss of control messages, it
        // is desirable that a message (plus the generic packet header) can fit
        // into a single MAC frame.

        var olsrHelloMessage = new OLSRHelloMessage(HELLO_INTERVAL, new ArrayList<>(grouped.values()));

        sim.broadcast(node, new OLSRPacket(node.getIP(), OLSRPacket.Type.HELLO, NEIGHB_HOLD_TIME, seqNum++, olsrHelloMessage), 1);
    }

    /**
     * A node records a set of "Link Tuples" (L_local_iface_addr,
     * L_neighbor_iface_addr, L_SYM_time, L_ASYM_time, L_time).
     * L_local_iface_addr is the interface neighborAddress of the local node (i.e.,
     * one endpoint of the link), L_neighbor_iface_addr is the interface
     * neighborAddress of the neighbor node (i.e., the other endpoint of the link),
     * L_SYM_time is the time until which the link is considered symmetric,
     * L_ASYM_time is the time until which the neighbor interface is
     * considered heard, and L_time specifies the time at which this record
     * expires and *MUST* be removed.  When L_SYM_time and L_ASYM_time are
     * expired, the link is considered lost.
     * <br><br>
     * This information is used when declaring the neighbor interfaces in
     * the HELLO messages.
     * <br><br>
     * L_SYM_time is used to decide the Link Type declared for the neighbor
     * interface.  If L_SYM_time is not expired, the link MUST be declared
     * symmetric.  If L_SYM_time is expired, the link MUST be declared
     * asymmetric.  If both L_SYM_time and L_ASYM_time are expired, the link
     * MUST be declared lost.
     */
    private class LinkSet {
        private final Map<IP, LinkSetEntry> entries = new HashMap<>();

        public Set<LinkSetEntry> getAll() {
            return new HashSet<>(entries.values());
        }

        public LinkSetEntry computeIfAbsent(IP originatorIP, Function<IP, LinkSetEntry> constructor) {
            return entries.computeIfAbsent(originatorIP, constructor);
        }

        public LinkSetEntry get(IP originatorIP) {
            return entries.get(originatorIP);
        }

        public boolean containsKey(IP originatorIP) {
            return entries.containsKey(originatorIP);
        }


        private static final class LinkSetEntry {
            private final IP otherNode;
            private long symTime;
            private long asymTime;
            private long time;

            private LinkSetEntry(IP otherNode, long symTime,   //time until the other node is considered symmetric
                                 long asymTime,  //time until the other node is considered heard
                                 long time       //time until to remove this entry
            ) {
                this.otherNode = otherNode;
                this.symTime = symTime;
                this.asymTime = asymTime;
                this.time = time;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) return true;
                if (obj == null || obj.getClass() != this.getClass()) return false;
                var that = (LinkSetEntry) obj;
                return Objects.equals(this.otherNode, that.otherNode) && this.symTime == that.symTime && this.asymTime == that.asymTime && this.time == that.time;
            }

            @Override
            public int hashCode() {
                return Objects.hash(otherNode, symTime, asymTime, time);
            }

            @Override
            public String toString() {
                return "LinkSetEntry[" + "otherNode=" + otherNode + ", " + "symTime=" + symTime + ", " + "asymTime=" + asymTime + ", " + "time=" + time + ']';
            }

        }
    }

    private final LinkSet linkSet = new LinkSet();


    /**
     * A node records a set of "neighbor tuples" (N_neighbor_main_addr,
     * N_status, N_willingness), describing neighbors.  N_neighbor_main_addr
     * is the main neighborAddress of a neighbor, N_status specifies if the node is
     * NOT_SYM or SYM.  N_willingness in an integer between 0 and 7, and
     * specifies the node's willingness to carry traffic on behalf of other
     * nodes.
     */
    private static class NeighborSet {
        private final Map<IP, NeighborSetEntry> entries = new HashMap<>();

        public Set<NeighborSetEntry> getAll() {
            return new HashSet<>(entries.values());
        }

        public NeighborSetEntry get(IP address) {
            return entries.get(address);
        }

        public void put(IP address, NeighborSetEntry entry) {
            entries.put(address, entry);
        }

        public boolean containsKey(IP otherNode) {
            return entries.containsKey(otherNode);
        }

        private static final class NeighborSetEntry {
            private final IP address;
            private Boolean isSym;
            private Willingness willingness;

            private NeighborSetEntry(IP address, Boolean isSym, Willingness willingness) {
                this.address = address;
                this.isSym = isSym;
                this.willingness = willingness;
            }


            @Override
            public boolean equals(Object obj) {
                if (obj == this) return true;
                if (obj == null || obj.getClass() != this.getClass()) return false;
                var that = (NeighborSetEntry) obj;
                return Objects.equals(this.address, that.address) && Objects.equals(this.isSym, that.isSym) && Objects.equals(this.willingness, that.willingness);
            }

            @Override
            public int hashCode() {
                return Objects.hash(address, isSym, willingness);
            }

            @Override
            public String toString() {
                return "NeighborSetEntry[" + "neighborAddress=" + address + ", " + "isSym=" + isSym + ", " + "willingness=" + willingness + ']';
            }

        }
    }

    private final NeighborSet neighborSet = new NeighborSet();

    /**
     * A node records a set of "2-hop tuples" (N_neighbor_main_addr,
     * N_2hop_addr, N_time), describing symmetric (and, since MPR links by
     * definition are also symmetric, thereby also MPR) links between its
     * neighbors and the symmetric 2-hop neighborhood.  N_neighbor_main_addr
     * is the main mainAddress of a neighbor, N_2hop_addr is the main mainAddress of
     * a 2-hop neighbor with a symmetric link to N_neighbor_main_addr, and
     * N_time specifies the time at which the tuple expires and *MUST* be
     * removed.
     * <br>
     * In a node, the set of 2-hop tuples are denoted the "2-hop Neighbor
     * Set".
     */
    private class TwoHopNeighborSet {
        private final Set<TwoHopNeighborSetEntry> entries = new HashSet<>();

        public void add(TwoHopNeighborSetEntry twoHopNeighborSetEntry) {
            entries.remove(twoHopNeighborSetEntry);
            entries.add(twoHopNeighborSetEntry);
        }

        public void removeIf(Function<TwoHopNeighborSetEntry, Boolean> condition) {
            entries.removeIf(condition::apply);
        }

        public Set<TwoHopNeighborSetEntry> getAll() {
            return new HashSet<>(entries);
        }

        /**
         * N2:
         * The set of 2-hop neighbors reachable from the interface
         * I, excluding:
         * <br>
         * (i)   the nodes only reachable by members of N with
         * willingness WILL_NEVER
         * <br>
         * (ii)  the node performing the computation
         * <br>
         * (iii) all the symmetric neighbors: the nodes for which
         * there exists a symmetric link to this node on some
         * interface.
         *
         * @return the set
         */
        public Set<TwoHopNeighborSetEntry> getN2Set() {
            Set<TwoHopNeighborSetEntry> set = new TreeSet<>();
            for (var entry : entries) {
                var neighborEntry = neighborSet.get(entry.neighborAddress);
                if (neighborEntry.willingness == Willingness.WILL_NEVER || !neighborEntry.isSym) continue;

                if (entry.twoHopAddress.equals(node.getIP())) continue;

                set.add(entry);
            }
            return set;
        }

        /**
         * @param neighborAddress the address of the neighbor
         * @param twoHopAddress   the address of the 2 hop neighbor
         * @param time            when this entry expires and must be removed
         */
        private record TwoHopNeighborSetEntry(IP neighborAddress, IP twoHopAddress, Long time) implements Comparable<TwoHopNeighborSetEntry> {

            @Override
            public boolean equals(Object obj) {
                if (obj == this) return true;
                if (obj == null || obj.getClass() != this.getClass()) return false;
                var that = (TwoHopNeighborSetEntry) obj;
                return Objects.equals(this.neighborAddress, that.neighborAddress) && Objects.equals(this.twoHopAddress, that.twoHopAddress);
            }

            @Override
            public int hashCode() {
                return Objects.hash(neighborAddress, twoHopAddress);
            }

            @Override
            public String toString() {
                return "TwoHopNeighborSetEntry[" + "neighborAddress=" + neighborAddress + ", " + "twoHopAddress=" + twoHopAddress + ", " + "time=" + time + ']';
            }

            @Override
            public int compareTo(@NotNull OLSRProtocol.TwoHopNeighborSet.TwoHopNeighborSetEntry o) {
                if (this.neighborAddress.compareTo(o.neighborAddress) == 0) return this.twoHopAddress.compareTo(o.twoHopAddress);
                return this.neighborAddress.compareTo(o.neighborAddress);
            }
        }
    }

    private final TwoHopNeighborSet twoHopNeighborSet = new TwoHopNeighborSet();

    /**
     * A node maintains a set of neighbors which are selected as MPR.  Their main addresses are listed in the MPR Set.
     */
    private static class MPRSet extends HashSet<IP> {
        public Set<IP> getAll() {
            return new HashSet<>(this);
        }
    }

    private final MPRSet mprSet = new MPRSet();

    /**
     * A node records a set of MPR-selector tuples (MS_main_addr, MS_time),
     * describing the neighbors which have selected this node as a MPR.
     * MS_main_addr is the main neighborAddress of a node, which has selected this
     * node as MPR.  MS_time specifies the time at which the tuple expires
     * and *MUST* be removed.
     * <br><br>
     * In a node, the set of MPR-selector tuples are denoted the "MPR
     * Selector Set".
     */
    private static class MPRSelectorSet {
        private final Map<IP, MPRSelectorSetEntry> entries = new HashMap<>();

        public boolean contains(IP address) {
            return entries.containsKey(address);
        }

        private static final class MPRSelectorSetEntry {
            private final IP address;
            private Long time;

            private MPRSelectorSetEntry(IP address, Long time) {
                this.address = address;
                this.time = time;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) return true;
                if (obj == null || obj.getClass() != this.getClass()) return false;
                var that = (MPRSelectorSetEntry) obj;
                return Objects.equals(this.address, that.address) && Objects.equals(this.time, that.time);
            }

            @Override
            public int hashCode() {
                return Objects.hash(address, time);
            }

            @Override
            public String toString() {
                return "MPRSelectorSetEntry[" + "address=" + address + ", " + "time=" + time + ']';
            }

        }
    }

    private final MPRSelectorSet mprSelectorSet = new MPRSelectorSet();

    private static class TopologySet {
        private Set<TopologySetEntry> entries = new HashSet<>();

        public void add(TopologySetEntry topologySetEntry) {
            entries.add(topologySetEntry);
        }

        public List<TopologySetEntry> getSetForLastAddress(IP lastAddress) {
            return entries.stream().filter(it -> it.lastAddress.equals(lastAddress)).toList();
        }

        public List<TopologySetEntry> getSetForAddress(IP address) {
            return entries.stream().filter(it -> it.lastAddress.equals(address)).toList();
        }

        public void removeIf(Function<TopologySetEntry, Boolean> filter) {
            entries.removeIf(filter::apply);
        }

        public List<TopologySetEntry> filteredList(Function<TopologySetEntry, Boolean> filter) {
            return entries.stream().filter(filter::apply).toList();
        }

        private static final class TopologySetEntry {
            private final IP address;
            private IP lastAddress;
            private Integer seqNumber;
            private Long time;

            private TopologySetEntry(IP address, IP lastAddress, Integer seqNumber, Long time) {
                this.address = address;
                this.lastAddress = lastAddress;
                this.seqNumber = seqNumber;
                this.time = time;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) return true;
                if (obj == null || obj.getClass() != this.getClass()) return false;
                var that = (TopologySetEntry) obj;
                return Objects.equals(this.address, that.address) && Objects.equals(this.lastAddress, that.lastAddress) && Objects.equals(this.seqNumber, that.seqNumber) && Objects.equals(this.time, that.time);
            }

            @Override
            public int hashCode() {
                return Objects.hash(address, lastAddress, seqNumber, time);
            }

            @Override
            public String toString() {
                return "TopologySetEntry[" + "address=" + address + ", " + "lastAddress=" + lastAddress + ", " + "seqNumber=" + seqNumber + ", " + "time=" + time + ']';
            }

        }
    }

    private final TopologySet topologySet = new TopologySet();

    public enum LinkType {
        UNSPEC_LINK, ASYM_LINK, SYM_LINK, LOST_LINK;

        @Override
        public String toString() {
            return switch (this) {
                case UNSPEC_LINK -> "Unspec.";
                case ASYM_LINK -> "Aysm.";
                case SYM_LINK -> "Sym.";
                case LOST_LINK -> "Lost";
            };
        }
    }

    public enum NeighborType {
        SYM_NEIGH, MPR_NEIGH, NOT_NEIGH;

        @Override
        public String toString() {
            return switch (this) {
                case SYM_NEIGH -> "Sym.";
                case MPR_NEIGH -> "MPR";
                case NOT_NEIGH -> "Not";
            };
        }
    }

    public enum Willingness {
        WILL_NEVER, WILL_DEFAULT, WILL_ALWAYS;

        @Override
        public String toString() {
            return switch (this) {
                case WILL_NEVER -> "Never";
                case WILL_DEFAULT -> "Default";
                case WILL_ALWAYS -> "Always";
            };
        }
    }
}
