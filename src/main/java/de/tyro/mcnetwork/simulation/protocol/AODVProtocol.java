package de.tyro.mcnetwork.simulation.protocol;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.network.payload.NewNetworkPacketPayload;
import de.tyro.mcnetwork.simulation.IHudRenderer;
import de.tyro.mcnetwork.simulation.INetworkNode;
import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.SimulationEngine;
import de.tyro.mcnetwork.simulation.packet.IApplicationPacket;
import de.tyro.mcnetwork.simulation.packet.INetworkPacket;
import de.tyro.mcnetwork.simulation.packet.IProtocolPaket;
import de.tyro.mcnetwork.simulation.packet.aodv.AODVRERRPacket;
import de.tyro.mcnetwork.simulation.packet.aodv.AODVRREPPacket;
import de.tyro.mcnetwork.simulation.packet.aodv.AODVRREQPacket;
import de.tyro.mcnetwork.simulation.packet.application.DestinationUnreachablePacket;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec2;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AODVProtocol implements IRoutingProtocol, IHudRenderer {
    static Logger log = LogUtils.getLogger();

    //CONSTANTS
    public static int ACTIVE_ROUTE_TIMEOUT = 3_000;
    public static int MY_ROUTE_TIMEOUT = 2 * ACTIVE_ROUTE_TIMEOUT;
    public static int NODE_TRAVERSAL_TIME = 5;
    public static int LOCAL_ADD_TTL = 2;
    public static int NET_DIAMETER = 35;
    public static int MAX_REPAIR_TTL = (int) (0.3 * NET_DIAMETER);
    public static int RREQ_RETRIES = 2;
    public static int RREQ_RATELIMIT = 10;
    public static int TIMEOUT_BUFFER = 2;
    public static int TTL_START = 1;
    public static int TTL_INCREMENT = 2;
    public static int TTL_THRESHOLD = 7;
    public static int NET_TRAVERSAL_TIME = 2 * NODE_TRAVERSAL_TIME * NET_DIAMETER;
    public static int PATH_DISCOVERY_TIME = 2 * NET_TRAVERSAL_TIME;
    public static int DELETE_PERIOD = 5 * ACTIVE_ROUTE_TIMEOUT;
    public static int RERR_RATELIMIT = 10;

    //LOCAL STATE
    private int sequenceNumber = 0;
    private int rreqId = 0;
    long nextRreqAttemptsResetInterval;
    int rreqInLastSecond;
    long nextRerrAttemptsResetInterval;
    int rerrInLastSecond;
    private final Map<IP, RouteEntry> routingTable = new ConcurrentHashMap<>();
    private final Map<String, Long> seenRreqs = new ConcurrentHashMap<>(); // originIP + RREQ_ID -> Arrival Time
    private final Map<IP, List<Pair<INetworkPacket, Integer>>> pendingData = new ConcurrentHashMap<>();
    private final INetworkNode node;
    private final Map<IP, RouteDiscoveryState> discoveries = new ConcurrentHashMap<>();
    private final TickActions tickActions = new TickActions();
    private final SimulationEngine simulator;
    private final ProtocolSettings settings;

    public AODVProtocol(INetworkNode node) {
        this.node = node;
        this.simulator = SimulationEngine.getInstance(node.getLevel().isClientSide());
        this.settings = new ProtocolSettings();

        settings.registerSetting("ACTIVE_ROUTE_TIMEOUT", Integer.class, () -> ACTIVE_ROUTE_TIMEOUT, v -> {
            ACTIVE_ROUTE_TIMEOUT = v;
            MY_ROUTE_TIMEOUT = 2 * v;
        });
        settings.registerSetting("MY_ROUTE_TIMEOUT", Integer.class, () -> MY_ROUTE_TIMEOUT);
        settings.registerSetting("NODE_TRAVERSAL_TIME", Integer.class, () -> NODE_TRAVERSAL_TIME, v -> {
            NODE_TRAVERSAL_TIME = v;
            NET_TRAVERSAL_TIME = 2 * v * NET_DIAMETER;
        });
        settings.registerSetting("LOCAL_ADD_TTL", Integer.class, () -> LOCAL_ADD_TTL, v -> LOCAL_ADD_TTL = v);
        settings.registerSetting("NET_DIAMETER", Integer.class, () -> NET_DIAMETER, v -> {
            NET_DIAMETER = v;
            NET_TRAVERSAL_TIME = 2 * NODE_TRAVERSAL_TIME * v;
            MAX_REPAIR_TTL = (int) (0.3 * v);
            PATH_DISCOVERY_TIME = 2 * NET_TRAVERSAL_TIME;
        });
        settings.registerSetting("MAX_REPAIR_TTL", Integer.class, () -> MAX_REPAIR_TTL);
        settings.registerSetting("RREQ_RETRIES", Integer.class, () -> RREQ_RETRIES, v -> RREQ_RETRIES = v);
        settings.registerSetting("RREQ_RATELIMIT", Integer.class, () -> RREQ_RATELIMIT, v -> RREQ_RATELIMIT = v);
        settings.registerSetting("TIMEOUT_BUFFER", Integer.class, () -> TIMEOUT_BUFFER, v -> TIMEOUT_BUFFER = v);
        settings.registerSetting("TTL_START", Integer.class, () -> TTL_START, v -> TTL_START = v);
        settings.registerSetting("TTL_INCREMENT", Integer.class, () -> TTL_INCREMENT, v -> TTL_INCREMENT = v);
        settings.registerSetting("TTL_THRESHOLD", Integer.class, () -> TTL_THRESHOLD, v -> TTL_THRESHOLD = v);
        settings.registerSetting("NET_TRAVERSAL_TIME", Integer.class, () -> NET_TRAVERSAL_TIME);
        settings.registerSetting("PATH_DISCOVERY_TIME", Integer.class, () -> PATH_DISCOVERY_TIME);
        settings.registerSetting("DELETE_PERIOD", Integer.class, () -> DELETE_PERIOD);
        settings.registerSetting("RERR_RATELIMIT", Integer.class, () -> RERR_RATELIMIT, v -> RERR_RATELIMIT = v);
    }

    @Override
    public INetworkNode getNode() {
        return node;
    }

    @Override
    public boolean hasRoute(IP destination) {
        var entry = routingTable.get(destination);
        return entry != null && entry.valid && entry.lifetime > simulator.getSimTime();
    }

    @Override
    public void discoverRoute(IP destIp) {
        // A node disseminates a RREQ when it determines that it needs a route
        // to a destination and does not have one available.  This can happen if
        // the destination is previously unknown to the node, or if a previously
        // valid route to the destination expires or is marked as invalid.
        if (hasRoute(destIp)) return;


        long now = simulator.getSimTime();

        // A node SHOULD NOT originate more than RREQ_RATELIMIT RREQ messages
        // per second.  After broadcasting a RREQ, a node waits for a RREP (or
        // other control message with current information regarding a route to
        // the appropriate destination).
        if (rreqInLastSecond >= RREQ_RATELIMIT) return;
        rreqInLastSecond++;

        // If a route is not received within
        // NET_TRAVERSAL_TIME milliseconds, the node MAY try again to discover a
        // route by broadcasting another RREQ, up to a maximum of RREQ_RETRIES
        // times at the maximum TTL value.  Each new attempt MUST increment and
        // update the RREQ ID.
        // To reduce congestion in a network, repeated attempts by a source node
        // at route discovery for a single destination MUST utilize a binary
        // exponential backoff.  The first time a source node broadcasts a RREQ,
        // it waits NET_TRAVERSAL_TIME milliseconds for the reception of a RREP.
        // If a RREP is not received within that time, the source node sends a
        // new RREQ.  When calculating the time to wait for the RREP after
        // sending the second RREQ, the source node MUST use a binary
        // exponential backoff.  Hence, the waiting time for the RREP
        // corresponding to the second RREQ is 2 * NET_TRAVERSAL_TIME
        // milliseconds.  If a RREP is not received within this time period,
        // another RREQ may be sent, up to RREQ_RETRIES additional attempts
        // after the first RREQ.  For each additional attempt, the waiting time
        // for the RREP is multiplied by 2, so that the time conforms to a
        // binary exponential backoff.
        RouteDiscoveryState state = discoveries.computeIfAbsent(destIp, d -> new RouteDiscoveryState());
        // Data packets waiting for a route (i.e., waiting for a RREP after a
        // RREQ has been sent) SHOULD be buffered.  The buffering SHOULD be
        // "first-in, first-out" (FIFO).
        // If a route discovery has been
        // attempted RREQ_RETRIES times at the maximum TTL without receiving any
        // RREP, all data packets destined for the corresponding destination
        // SHOULD be dropped from the buffer and a Destination Unreachable
        // message SHOULD be delivered to the application.
        if (state.rreqInLastSecond > RREQ_RETRIES && state.ttl >= NET_DIAMETER) {
            dropBufferedPackets(destIp);
            notifyDestinationUnreachable(destIp);
            discoveries.remove(destIp);
            return;
        }
        tickActions.add(now + (long) (int) Math.pow(2, state.rreqAttempts) * NET_TRAVERSAL_TIME, () -> discoverRoute(destIp));
        state.rreqAttempts++;

        // Data packets waiting for a route (i.e., waiting for a RREP after a
        // RREQ has been sent) SHOULD be buffered.  The buffering SHOULD be
        // "first-in, first-out" (FIFO).


        // For each attempt, the TTL field of the IP header
        // is set according to the mechanism specified in section 6.4, in order
        // to enable control over how far the RREQ is disseminated for the each
        // retry. ->

        // The Hop Count stored in an invalid routing table entry indicates the
        // last known hop count to that destination in the routing table.  When
        // a new route to the same destination is required at a later time
        // (e.g., upon route loss), the TTL in the RREQ IP header is initially
        // set to the Hop Count plus TTL_INCREMENT.  Thereafter, following each
        // timeout the TTL is incremented by TTL_INCREMENT until TTL =
        // TTL_THRESHOLD is reached.  Beyond this TTL = NET_DIAMETER is used.
        // Once TTL = NET_DIAMETER, the timeout for waiting for the RREP is set
        // to NET_TRAVERSAL_TIME, as specified in section 6.3.
        var entry = routingTable.get(destIp);
        if (state.ttl == 0 && entry != null && entry.hopCount > 0) {
            state.ttl = entry.hopCount + TTL_INCREMENT;
        }
        // To prevent unnecessary network-wide dissemination of RREQs, the
        // originating node SHOULD use an expanding ring search technique.  In
        // an expanding ring search, the originating node initially uses a TTL =
        // TTL_START in the RREQ packet IP header and sets the timeout for
        // receiving a RREP to RING_TRAVERSAL_TIME milliseconds.
        // RING_TRAVERSAL_TIME is calculated as described in section 10.  The
        // TTL_VALUE used in calculating RING_TRAVERSAL_TIME is set equal to the
        // value of the TTL field in the IP header.  If the RREQ times out
        // without a corresponding RREP, the originator broadcasts the RREQ
        // again with the TTL incremented by TTL_INCREMENT.  This continues
        // until the TTL set in the RREQ reaches TTL_THRESHOLD, beyond which a
        // TTL = NET_DIAMETER is used for each attempt.  Each time, the timeout
        // for receiving a RREP is RING_TRAVERSAL_TIME.  When it is desired to
        // have all retries traverse the entire ad hoc network, this can be
        // achieved by configuring TTL_START and TTL_INCREMENT both to be the
        // same value as NET_DIAMETER.
        else if (state.ttl == 0) {
            state.ttl = TTL_START;
        } else if (state.ttl < TTL_THRESHOLD) {
            state.ttl += TTL_INCREMENT;
        } else {
            state.ttl = NET_DIAMETER;
        }


        // The Destination Sequence Number field in the RREQ message is the last
        // known destination sequence number for this destination and is copied
        // from the Destination Sequence Number field in the routing table.  If
        // no sequence number is known, the unknown sequence number flag MUST be
        // set.  The Originator Sequence Number in the RREQ message is the
        // node's own sequence number, which is incremented prior to insertion
        // in a RREQ.  The RREQ ID field is incremented by one from the last
        // RREQ ID used by the current node.  Each node maintains only one RREQ
        // ID.  The Hop Count field is set to zero.

        sequenceNumber++;
        rreqId++;

        RouteEntry destEntry = routingTable.get(destIp);
        int destSeqNum = destEntry == null ? 0 : destEntry.seqNumber;

        var rreq = new AODVRREQPacket(node.getIP(), destIp, false, false, true, false, destEntry == null, 0, rreqId, destSeqNum, sequenceNumber);
        // An originating node often expects to have bidirectional
        // communications with a destination node.  In such cases, it is not
        // sufficient for the originating node to have a route to the
        // destination node; the destination must also have a route back to the
        // originating node.  In order for this to happen as efficiently as
        // possible, any generation of a RREP by an intermediate node (as in
        // section 6.6) for delivery to the originating node SHOULD be
        // accompanied by some action that notifies the destination about a
        // route back to the originating node.  The originating node selects
        // this mode of operation in the intermediate nodes by setting the 'G'
        // flag.  See section 6.6.3 for details about actions taken by the
        // intermediate node in response to a RREQ with the 'G' flag set.i

        // --- I just always set the G flag ig?

        // Before broadcasting the RREQ, the originating node buffers the RREQ
        // ID and the Originator IP address (its own address) of the RREQ for
        // PATH_DISCOVERY_TIME.  In this way, when the node receives the packet
        // again from its neighbors, it will not reprocess and re-forward the
        // packet.
        seenRreqs.put(key(node.getIP(), rreqId), now);
        state.rreqInLastSecond++;
        simulator.broadcast(node, rreq, state.ttl);


    }

    private void notifyDestinationUnreachable(IP destIp) {
        NewNetworkPacketPayload.sendToSelf(new DestinationUnreachablePacket(node.getIP(), destIp), node);
    }

    private void dropBufferedPackets(IP destIp) {
        pendingData.remove(destIp);
    }

    @Override
    public void onProtocolPacketReceived(IProtocolPaket packet) {
        if (packet instanceof AODVRREQPacket rreq) handleRREQ(rreq);
        else if (packet instanceof AODVRREPPacket rrep) handleRREP(rrep);
        else if (packet instanceof AODVRERRPacket rerr) handleRERR(3, null, rerr);
    }

    private void handleRREP(AODVRREPPacket rrep) {
        //  When a node receives a RREP message, it searches (using longest-
        //  prefix matching) for a route to the previous hop.  If needed, a route
        //  is created for the previous hop, but without a valid sequence number
        //  (see section 6.2).
        var previousIP = rrep.getNetworkFrame().getFrom().getIP();
        var previousHopEntry = routingTable.get(previousIP);
        if (previousHopEntry == null) {
            previousHopEntry = new RouteEntry(previousIP);
            previousHopEntry.seqValid = false;
            previousHopEntry.nextHop = previousIP;
            previousHopEntry.lifetime = simulator.getSimTime() + ACTIVE_ROUTE_TIMEOUT;
            previousHopEntry.valid = true;
            previousHopEntry.hopCount = 1;
            routingTable.put(previousIP, previousHopEntry);
        }

        // Next, the node then increments the hop count value in the RREP by one, to account for the new hop through the intermediate node.
        rrep.hopCount++;

        var destinationEntry = routingTable.get(rrep.getDestinationIP());
        // Then the forward route for this destination is created if it does not already exist.
        if (destinationEntry == null) {
            destinationEntry = new RouteEntry(rrep.getDestinationIP());
            routingTable.put(rrep.getDestinationIP(), destinationEntry);
        }

        // Otherwise, the node compares the Destination Sequence
        // Number in the message with its own stored destination sequence number
        // for the Destination IP Address in the RREP message.  Upon comparison,
        // the existing entry is updated only in the following circumstances:
        // (i)       the sequence number in the routing table is marked as
        //           invalid in route table entry.
        // (ii)      the Destination Sequence Number in the RREP is greater than
        //           the node's copy of the destination sequence number and the
        //           known value is valid, or
        // (iii)     the sequence numbers are the same, but the route is is
        //           marked as inactive, or
        // (iv)      the sequence numbers are the same, and the New Hop Count is
        //           smaller than the hop count in route table entry.
        //  If the route table entry to the destination is created or updated,
        //  then the following actions occur:
        //
        //  -  the route is marked as active,
        //
        //  -  the destination sequence number is marked as valid,
        //
        //  -  the next hop in the route entry is assigned to be the node from
        //     which the RREP is received, which is indicated by the source IP
        //     address field in the IP header,
        //
        //  -  the hop count is set to the value of the New Hop Count,
        //
        //  -  the expiry time is set to the current time plus the value of the
        //     Lifetime in the RREP message,
        //
        //  -  and the destination sequence number is the Destination Sequence
        //     Number in the RREP message.
        //
        //  The current node can subsequently use this route to forward data
        //  packets to the destination.
        if (!destinationEntry.seqValid || rrep.destSeqNumber > destinationEntry.seqNumber || (rrep.destSeqNumber == destinationEntry.seqNumber && (!destinationEntry.valid || rrep.hopCount < destinationEntry.hopCount))) {
            destinationEntry.nextHop = previousIP;
            destinationEntry.hopCount = rrep.hopCount;
            destinationEntry.seqNumber = rrep.destSeqNumber;
            destinationEntry.seqValid = true;
            destinationEntry.valid = true;
            destinationEntry.lifetime = simulator.getSimTime() + rrep.lifetime;
        }


        if (node.getIP().equals(rrep.getOriginatorIP())) {
            discoveries.remove(rrep.getDestinationIP());
            var pending = pendingData.remove(rrep.getDestinationIP());
            if (pending != null) pending.forEach(it -> send(it.getFirst(), it.getSecond()));
            return;
        }

        // If the current node is not the node indicated by the Originator IP
        // Address in the RREP message AND a forward route has been created or
        // updated as described above, the node consults its route table entry
        // for the originating node to determine the next hop for the RREP
        // packet, and then forwards the RREP towards the originator using the
        // information in that route table entry.  If a node forwards a RREP
        // over a link that is likely to have errors or be unidirectional, the
        // node SHOULD set the 'A' flag to require that the recipient of the
        // RREP acknowledge receipt of the RREP by sending a RREP-ACK message
        // back (see section 6.8).
        var reverseEntry = routingTable.get(rrep.getOriginatorIP());
        IP nextHopToOriginator = reverseEntry.nextHop;

        // When any node transmits a RREP, the precursor list for the
        // corresponding destination node is updated by adding to it the next
        // hop node to which the RREP is forwarded.  Also, at each node the
        // (reverse) route used to forward a RREP has its lifetime changed to be
        // the maximum of (existing-lifetime, (current time +
        // ACTIVE_ROUTE_TIMEOUT).  Finally, the precursor list for the next hop
        // towards the destination is updated to contain the next hop towards
        // the source.
        destinationEntry.precursors.add(nextHopToOriginator);
        reverseEntry.lifetime = Math.max(reverseEntry.lifetime, simulator.getSimTime() + ACTIVE_ROUTE_TIMEOUT);
        var nextHopEntry = routingTable.get(nextHopToOriginator);
        nextHopEntry.precursors.add(destinationEntry.nextHop);

        simulator.unicast(node, simulator.getNode(nextHopToOriginator), rrep, rrep.getNetworkFrame().getTtl() - 1);
    }

    private void generateRREP(AODVRREQPacket rreq) {
        // A node generates a RREP if either:
        // (i)       it is itself the destination, or
        // (ii)      it has an active route to the destination, the destination
        //           sequence number in the node's existing route table entry
        //           for the destination is valid and greater than or equal to
        //           the Destination Sequence Number of the RREQ (comparison
        //           using signed 32-bit arithmetic), and the "destination only"
        //           ('D') flag is NOT set.
        // When generating a RREP message, a node copies the Destination IP
        // Address and the Originator Sequence Number from the RREQ message into
        // the corresponding fields in the RREP message.  Processing is slightly
        // different, depending on whether the node is itself the requested
        // destination (see section 6.6.1), or instead if it is an intermediate
        // node with an fresh enough route to the destination (see section
        // 6.6.2).

        var destinationEntry = routingTable.get(rreq.getDestinationIP());
        var originatorEntry = routingTable.get(rreq.getOriginatorIP());
        boolean isDestination = rreq.getDestinationIP().equals(node.getIP());
        boolean isIntermediate = !isDestination && destinationEntry != null && destinationEntry.valid && destinationEntry.seqValid && destinationEntry.seqNumber >= rreq.destinationSequenceNumber && !rreq.destinationOnlyFlag;

        if (!isDestination && !isIntermediate) return;

        AODVRREPPacket rrep = new AODVRREPPacket(rreq.getOriginatorIP(), rreq.getDestinationIP(), false, false, 0, 0, 0);

        // If the generating node is the destination itself, it MUST increment
        // its own sequence number by one if the sequence number in the RREQ
        // packet is equal to that incremented value.  Otherwise, the
        // destination does not change its sequence number before generating the
        // RREP message. The destination node places its (perhaps newly
        // incremented) sequence number into the Destination Sequence Number
        // field of the RREP, and enters the value zero in the Hop Count field
        // of the RREP.
        //
        // The destination node copies the value MY_ROUTE_TIMEOUT (see section
        // 10) into the Lifetime field of the RREP.  Each node MAY reconfigure
        // its value for MY_ROUTE_TIMEOUT, within mild constraints (see section
        // 10).
        if (isDestination) {
            if (sequenceNumber + 1 == rreq.destinationSequenceNumber) sequenceNumber++;
            rrep.destSeqNumber = sequenceNumber;
            rrep.lifetime = MY_ROUTE_TIMEOUT;
        }
        // 6.6.2. Route Reply Generation by an Intermediate Node
        //
        // If the node generating the RREP is not the destination node, but
        // instead is an intermediate hop along the path from the originator to
        // the destination, it copies its known sequence number for the
        // destination into the Destination Sequence Number field in the RREP
        // message.
        //
        // The intermediate node updates the forward route entry by placing the
        // last hop node (from which it received the RREQ, as indicated by the
        // source IP address field in the IP header) into the precursor list for
        // the forward route entry -- i.e., the entry for the Destination IP
        // Address.  The intermediate node also updates its route table entry
        // for the node originating the RREQ by placing the next hop towards the
        // destination in the precursor list for the reverse route entry --
        // i.e., the entry for the Originator IP Address field of the RREQ
        // message data.
        //
        // The intermediate node places its distance in hops from the
        // destination (indicated by the hop count in the routing table) Count
        // field in the RREP.  The Lifetime field of the RREP is calculated by
        // subtracting the current time from the expiration time in its route
        // table entry.
        else {
            rrep.destSeqNumber = destinationEntry.seqNumber;
            rrep.hopCount = destinationEntry.hopCount;
            rrep.lifetime = destinationEntry.lifetime - simulator.getSimTime();

            destinationEntry.precursors.add(rreq.getNetworkFrame().getFrom().getIP());
            originatorEntry.precursors.add(destinationEntry.nextHop);
        }

        // Once created, the RREP is unicast to the next hop toward the
        // originator of the RREQ, as indicated by the route table entry for
        // that originator.  As the RREP is forwarded back towards the node
        // which originated the RREQ message, the Hop Count field is incremented
        // by one at each hop.  Thus, when the RREP reaches the originator, the
        // Hop Count represents the distance, in hops, of the destination from
        // the originator.
        simulator.unicast(node, simulator.getNode(originatorEntry.nextHop), rrep, rreq.hopCount * 2);


        // After a node receives a RREQ and responds with a RREP, it discards
        // the RREQ.  If the RREQ has the 'G' flag set, and the intermediate
        // node returns a RREP to the originating node, it MUST also unicast a
        // gratuitous RREP to the destination node.  The gratuitous RREP that is
        // to be sent to the desired destination contains the following values
        // in the RREP message fields:
        //
        // Hop Count                        The Hop Count as indicated in the
        //                                  node's route table entry for the
        //                                  originator
        //
        // Destination IP Address           The IP address of the node that
        //                                  originated the RREQ
        //
        // Destination Sequence Number      The Originator Sequence Number from
        //                                  the RREQ
        //
        // Originator IP Address            The IP address of the Destination
        //                                  node in the RREQ
        //
        // Lifetime                         The remaining lifetime of the route
        //                                  towards the originator of the RREQ,
        //                                  as known by the intermediate node.
        //
        // The gratuitous RREP is then sent to the next hop along the path to
        // the destination node, just as if the destination node had already
        // issued a RREQ for the originating node and this RREP was produced in
        // response to that (fictitious) RREQ.  The RREP that is sent to the
        // originator of the RREQ is the same whether or not the 'G' bit is set.
        if (isIntermediate && rreq.gratuitousFlag) {
            var gRrep = new AODVRREPPacket(rreq.getDestinationIP(), rreq.getOriginatorIP(), false, false, originatorEntry.hopCount, rreq.originatorSequenceNumber, originatorEntry.lifetime);
            simulator.unicast(node, simulator.getNode(destinationEntry.nextHop), gRrep, 100);
        }
    }

    private void handleRREQ(AODVRREQPacket rreq) {
        // When a node receives a RREQ, it first creates or updates a route to
        // the previous hop without a valid sequence number (see section 6.2)
        var lastHop = rreq.getNetworkFrame().getFrom().getIP();
        var reverseEntry = routingTable.computeIfAbsent(lastHop, RouteEntry::new);
        reverseEntry.seqValid = false;
        reverseEntry.nextHop = lastHop;
        reverseEntry.valid = true;
        reverseEntry.lifetime = simulator.getSimTime() + ACTIVE_ROUTE_TIMEOUT;

        // then checks to determine whether it has received a RREQ with the same
        // Originator IP Address and RREQ ID within at least the last
        // PATH_DISCOVERY_TIME.  If such a RREQ has been received, the node
        // silently discards the newly received RREQ.  The rest of this
        // subsection describes actions taken for RREQs that are not discarded.
        String key = key(rreq.getOriginatorIP(), rreq.rreqId);
        if (seenRreqs.containsKey(key)) return;
        seenRreqs.put(key, simulator.getSimTime());

        // First, it first increments the hop count value in the RREQ by one, to
        // account for the new hop through the intermediate node.
        rreq.hopCount++;

        // Then the node searches for a reverse route to the Originator IP Address (see
        // section 6.2), using longest-prefix matching.  If need be, the route
        // is created, or updated using the Originator Sequence Number from the
        // RREQ in its routing table.  This reverse route will be needed if the
        // node receives a RREP back to the node that originated the RREQ
        // (identified by the Originator IP Address).  When the reverse route is
        // created or updated, the following actions on the route are also
        // carried out:
        //
        // 1. the Originator Sequence Number from the RREQ is compared to the
        //    corresponding destination sequence number in the route table entry
        //    and copied if greater than the existing value there
        //
        // 2. the valid sequence number field is set to true;
        //
        // 3. the next hop in the routing table becomes the node from which the
        //    RREQ was received (it is obtained from the source IP address in
        //    the IP header and is often not equal to the Originator IP Address
        //    field in the RREQ message);
        //
        //
        //
        // 4. the hop count is copied from the Hop Count in the RREQ message;
        //
        // Whenever a RREQ message is received, the Lifetime of the reverse
        // route entry for the Originator IP address is set to be the maximum of
        // (ExistingLifetime, MinimalLifetime), where
        //
        //    MinimalLifetime =    (current time + 2*NET_TRAVERSAL_TIME -
        //                         2*HopCount*NODE_TRAVERSAL_TIME).
        //
        // The current node can use the reverse route to forward data packets in
        // the same way as for any other route in the routing table.
        var originator = rreq.getOriginatorIP();
        var revRoute = routingTable.computeIfAbsent(originator, RouteEntry::new);

        if (!revRoute.seqValid || rreq.originatorSequenceNumber > revRoute.seqNumber) {
            revRoute.seqNumber = rreq.originatorSequenceNumber;
            revRoute.seqValid = true;
        }

        revRoute.nextHop = lastHop;
        revRoute.hopCount = rreq.hopCount;
        revRoute.valid = true;

        long minimalLifetime = simulator.getSimTime() + 2L * NET_TRAVERSAL_TIME - 2L * rreq.hopCount * NODE_TRAVERSAL_TIME;

        revRoute.lifetime = Math.max(revRoute.lifetime, minimalLifetime);


        // If a node does generate a RREP, then the node discards the
        // RREQ.  Notice that, if intermediate nodes reply to every transmission
        // of RREQs for a particular destination, it might turn out that the
        // destination does not receive any of the discovery messages.  In this
        // situation, the destination does not learn of a route to the
        // originating node from the RREQ messages.  This could cause the
        // destination to initiate a route discovery (for example, if the
        // originator is attempting to establish a TCP session).  In order that
        // the destination learn of routes to the originating node, the
        // originating node SHOULD set the "gratuitous RREP" ('G') flag in the
        // RREQ if for any reason the destination is likely to need a route to
        // the originating node.  If, in response to a RREQ with the 'G' flag
        // set, an intermediate node returns a RREP, it MUST also unicast a
        // gratuitous RREP to the destination node (see section 6.6.3).

        boolean generateRREP = false;
        RouteEntry destRoute = routingTable.get(rreq.getDestinationIP());

        // Case 1: this node is the destination
        if (node.getIP().equals(rreq.getDestinationIP())) {
            generateRREP = true;
        }

        // Case 2: intermediate node with a fresh enough route
        else if (!rreq.destinationOnlyFlag && destRoute != null && destRoute.valid) {
            if (destRoute.seqValid && destRoute.seqNumber >= rreq.destinationSequenceNumber) {
                generateRREP = true;
            }
        }

        if (generateRREP) {
            generateRREP(rreq);
            return; // RREQ is discarded
        }

        // If a node does not generate a RREP (following the processing rules in
        // section 6.6), and if the incoming IP header has TTL larger than 1,
        // the node updates and broadcasts the RREQ to address 255.255.255.255
        // on each of its configured interfaces (see section 6.14).  To update
        // the RREQ, the TTL or hop limit field in the outgoing IP header is
        // decreased by one, and the Hop Count field in the RREQ message is
        // incremented by one, to account for the new hop through the
        // intermediate node.  Lastly, the Destination Sequence number for the
        // requested destination is set to the maximum of the corresponding
        // value received in the RREQ message, and the destination sequence
        // value currently maintained by the node for the requested destination.
        // However, the forwarding node MUST NOT modify its maintained value for
        // the destination sequence number, even if the value received in the
        // incoming RREQ is larger than the value currently maintained by the
        // forwarding node.
        if (rreq.getNetworkFrame().getTtl() <= 1) return;

        if (destRoute != null && destRoute.valid) {
            rreq.destinationSequenceNumber = Math.max(rreq.destinationSequenceNumber, destRoute.seqNumber);
        }

        simulator.broadcast(node, rreq.hop(node), rreq.getNetworkFrame().getTtl() - 1);
    }

    private void handleRERR(int rerrType, IP unreachableNode, AODVRERRPacket rerr) {
        // Generally, route error and link breakage processing requires the
        // following steps:
        //
        // -  Invalidating existing routes
        //
        // -  Listing affected destinations
        //
        // -  Determining which, if any, neighbors may be affected
        //
        // -  Delivering an appropriate RERR to such neighbors
        //
        // A Route Error (RERR) message MAY be either broadcast (if there are
        // many precursors), unicast (if there is only 1 precursor), or
        // iteratively unicast to all precursors (if broadcast is
        // inappropriate).  Even when the RERR message is iteratively unicast to
        // several precursors, it is considered to be a single control message
        // for the purposes of the description in the text that follows.  With
        // that understanding, a node SHOULD NOT generate more than
        // RERR_RATELIMIT RERR messages per second.
        var now = simulator.getSimTime();

        if (rerrInLastSecond > RERR_RATELIMIT) return;
        rerrInLastSecond++;

        // A node initiates processing for a RERR message in three situations:
        //
        // (i)       if it detects a link break for the next hop of an active
        //           route in its routing table while transmitting data (and
        //           route repair, if attempted, was unsuccessful), or
        //
        // (ii)      if it gets a data packet destined to a node for which it
        //           does not have an active route and is not repairing (if
        //           using local repair), or
        //
        // (iii)     if it receives a RERR from a neighbor for one or more
        //           active routes.
        //
        // For case (i), the node first makes a list of unreachable destinations
        // consisting of the unreachable neighbor and any additional
        // destinations (or subnets, see section 7) in the local routing table
        // that use the unreachable neighbor as the next hop.  In this case, if
        // a subnet route is found to be newly unreachable, an IP destination
        // address for the subnet is constructed by appending zeroes to the
        // bnet prefix as shown in the route table entry.  This is
        // unambiguous, since the precursor is known to have route table
        // information with a compatible prefix length for that subnet.
        // For case (ii), there is only one unreachable destination, which is
        // the destination of the data packet that cannot be delivered.  For
        // case (iii), the list should consist of those destinations in the RERR
        // for which there exists a corresponding entry in the local routing
        // table that has the transmitter of the received RERR as the next hop.

        List<RouteEntry> unreachableEntries = List.of();

        if (rerrType == 1) {
            unreachableEntries = routingTable.values().stream().filter(entry -> entry.destination.equals(unreachableNode) || entry.nextHop.equals(unreachableNode)).toList();

        } else if (rerrType == 2) {
            unreachableEntries = List.of(routingTable.get(unreachableNode));
        } else if (rerrType == 3) {
            unreachableEntries = routingTable.values().stream().filter(entry -> rerr.unreachable.containsKey(entry.destination) && entry.nextHop == rerr.getNetworkFrame().getFrom().getIP()).toList();
        }

        // Some of the unreachable destinations in the list could be used by
        // neighboring nodes, and it may therefore be necessary to send a (new)
        // RERR.  The RERR should contain those destinations that are part of
        // the created list of unreachable destinations and have a non-empty
        // precursor list.

        var newRerr = new AODVRERRPacket(node.getIP(), null, false);
        var numberOfDestinations = 0;
        for (var entry : unreachableEntries) {
            if (entry.precursors.isEmpty()) continue;

            newRerr.unreachable.put(entry.destination, entry.seqNumber);
            numberOfDestinations += entry.precursors.size();
        }

        // The neighboring node(s) that should receive the RERR are all those
        // that belong to a precursor list of at least one of the unreachable
        // destination(s) in the newly created RERR.  In case there is only one
        // unique neighbor that needs to receive the RERR, the RERR SHOULD be
        // unicast toward that neighbor.  Otherwise the RERR is typically sent
        // to the local broadcast address (Destination IP == 255.255.255.255,
        // TTL == 1) with the unreachable destinations, and their corresponding
        // destination sequence numbers, included in the packet.  The DestCount
        // field of the RERR packet indicates the number of unreachable
        // destinations included in the packet.

        IP unicastAddress = null;
        if (numberOfDestinations == 1) {
            unicastAddress = unreachableEntries.getFirst().precursors.toArray(new IP[]{})[0];

            newRerr.setDestinationIP(unreachableEntries.getFirst().destination);
        } else if (numberOfDestinations > 1) {
            newRerr.setDestinationIP(IP.BROADCAST);
        }

        // Just before transmitting the RERR, certain updates are made on the
        // routing table that may affect the destination sequence numbers for
        // the unreachable destinations.  For each one of these destinations,
        // the corresponding routing table entry is updated as follows:
        //
        // 1. The destination sequence number of this routing entry, if it
        //    exists and is valid, is incremented for cases (i) and (ii) above,
        //    and copied from the incoming RERR in case (iii) above.
        // 2. The entry is invalidated by marking the route entry as invalid
        //
        // 3. The Lifetime field is updated to current time plus DELETE_PERIOD.
        //    Before this time, the entry SHOULD NOT be deleted.

        // Note that the Lifetime field in the routing table plays dual role --
        // for an active route it is the expiry time, and for an invalid route
        // it is the deletion time.  If a data packet is received for an invalid
        // route, the Lifetime field is updated to current time plus
        // DELETE_PERIOD.  The determination of DELETE_PERIOD is discussed in
        // Section 10.
        unreachableEntries.forEach(entry -> {
            if (rerrType == 1 || rerrType == 2) {
                entry.seqNumber++;
            } else if (rerrType == 3) {
                entry.seqNumber = rerr.unreachable.get(entry.destination);
            }
            entry.valid = false;
            entry.lifetime = simulator.getSimTime() + DELETE_PERIOD;
        });


        if (unicastAddress != null) {
            simulator.unicast(node, simulator.getNode(unicastAddress), newRerr, 1);
        } else if (newRerr.destCount() != 0) {
            simulator.broadcast(node, newRerr, 1);
        }
    }

    private static String key(IP ip, int id) {
        return ip.toString() + ":" + id;
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

        //we want to send this somewhere else. check if we have a destination, and either send it, or store it for later
        var entry = routingTable.get(packet.getDestinationIP());
        if (hasRoute(packet.getDestinationIP())) {

            // (i)       if it detects a link break for the next hop of an active
            //           route in its routing table while transmitting data (and
            //           route repair, if attempted, was unsuccessful), or
            if (entry != null && entry.valid && !simulator.nodeExists(entry.nextHop)) {
                handleRERR(1, packet.getDestinationIP(), null);
                return;
            }

            var nextNode = simulator.getNode(entry.nextHop);
            if (simulator.unicast(node, nextNode, packet, ttl)) {
                entry.lifetime = simulator.getSimTime() + MY_ROUTE_TIMEOUT;
            }
            return;
        }


        // (ii)      if it gets a data packet destined to a node for which it
        //           does not have an active route and is not repairing (if
        //           using local repair), or
        if (!packet.getOriginatorIP().equals(node.getIP()) && (entry == null || !entry.valid)) {
            handleRERR(2, packet.getDestinationIP(), null);
            return;
        }

        pendingData.computeIfAbsent(packet.getDestinationIP(), k -> new ArrayList<>()).add(new Pair<>(packet, ttl));
        discoverRoute(packet.getDestinationIP());
    }

    public Vec2 getRenderSize(Font font) {
        var height = 35;
        height += 8 * Math.max(1, routingTable.size());
        height += 14;
        height += 8 * Math.max(1, discoveries.size());
        height += 14;
        height += 8 * Math.max(1, discoveries.size());
        height += 11;

        return new Vec2(200, height);
    }

    @Override
    public void render(RenderUtil renderer) {
        var width = getRenderSize(renderer.getFont()).x;
        float y = 0f;

        long now = SimulationEngine.getInstance(true).getSimTime();

        renderer.renderHLineWithAlphaColor(width, y);
        y += 4;

        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "Seq=" + sequenceNumber, width, y);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, "RREQ/s=" + rreqInLastSecond, width, y);
        y += 9;

        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "RREQ_ID=" + rreqId, width, y);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, "RERR/s=" + rerrInLastSecond, width, y);
        y += 10;

        renderer.renderHLineWithAlphaColor(width, y);
        y += 2;

        renderer.drawString(RenderUtil.Align.CENTER, "Routing Table", 0xAAAAFF, width, y);
        y += 10;

        if (routingTable.isEmpty()) {
            renderer.drawStringWithAlphaColor(RenderUtil.Align.CENTER, "<empty>", width, y);
            y += 8;
        } else {
            for (RouteEntry entry : routingTable.values()) {

                int color;

                if (!entry.valid) {
                    color = 0xFF5555; // red
                } else if (entry.lifetime - now < 2000) {
                    color = 0xFFAA00; // expiring
                } else {
                    color = 0x55FF55; // valid
                }

                String line = entry.destination + " → " + entry.nextHop + "  h=" + entry.hopCount + "  seq=" + entry.seqNumber + "  lt=" + Math.max(0, entry.lifetime - now);

                renderer.drawString(RenderUtil.Align.LEFT, line, color, width, y);

                y += 8;
            }
        }

        y += 2;
        renderer.renderHLineWithAlphaColor(width, y);
        y += 2;

        renderer.drawString(RenderUtil.Align.CENTER, "Discoveries", 0xFFFF55, width, y);
        y += 10;

        if (discoveries.isEmpty()) {
            renderer.drawStringWithAlphaColor(RenderUtil.Align.CENTER, "<none>", width, y);
            y += 8;
        } else {
            for (Map.Entry<IP, RouteDiscoveryState> e : discoveries.entrySet()) {

                RouteDiscoveryState d = e.getValue();

                String line = e.getKey() + "  ttl=" + d.ttl + "  tries=" + d.rreqAttempts + "  r/s=" + d.rreqInLastSecond;

                renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, line, width, y);

                y += 8;
            }
        }

        y += 2;
        renderer.renderHLineWithAlphaColor(width, y);
        y += 2;

        renderer.drawString(RenderUtil.Align.CENTER, "Pending Data", 0xFFAAFF, width, y);
        y += 10;

        if (pendingData.isEmpty()) {
            renderer.drawStringWithAlphaColor(RenderUtil.Align.CENTER, "<none>", width, y);
            y += 8;
        } else {
            for (Map.Entry<IP, List<Pair<INetworkPacket, Integer>>> e : pendingData.entrySet()) {

                renderer.drawString(RenderUtil.Align.LEFT, e.getKey() + "  queued=" + e.getValue().size(), 0xFFAAFF, width, y);

                y += 8;
            }
        }

        y += 2;
        renderer.renderHLineWithAlphaColor(width, y);
        y += 2;


        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "Seen RREQs=" + seenRreqs.size(), width, y);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, "TickActions=" + tickActions.size(), width, y);
    }

    @Override
    public void simTick() {
        long now = simulator.getSimTime();

        tickActions.execute(now);


        if (nextRreqAttemptsResetInterval <= now) {
            rreqInLastSecond = 0;
            nextRreqAttemptsResetInterval = now + 1000;
        }

        if (nextRerrAttemptsResetInterval >= now) {
            nextRerrAttemptsResetInterval = now + 1000;
            rerrInLastSecond = 0;
        }

        for (Map.Entry<IP, RouteDiscoveryState> entry : this.discoveries.entrySet()) {
            RouteDiscoveryState routeDiscoveryState = entry.getValue();
            if (routeDiscoveryState.nextRreqAttemptsResetInterval <= now) {
                routeDiscoveryState.nextRreqAttemptsResetInterval = now + 1000;
                routeDiscoveryState.rreqInLastSecond = 0;
            }
        }


        // An expired routing table entry SHOULD NOT be expunged before
        // (current_time + DELETE_PERIOD) (see section 6.11).  Otherwise, the
        // soft state corresponding to the route (e.g., last known hop count)
        // will be lost.  Furthermore, a longer routing table entry expunge time
        // MAY be configured.  Any routing table entry waiting for a RREP SHOULD
        // NOT be expunged before (current_time + 2 * NET_TRAVERSAL_TIME).
        routingTable.values().forEach(e -> {
            if (e.lifetime < now) e.valid = false;
        });
        routingTable.values().removeIf(e -> !e.valid && e.lifetime + DELETE_PERIOD < now);
        seenRreqs.entrySet().removeIf(e -> e.getValue() + PATH_DISCOVERY_TIME < now);
    }

    private static class RouteEntry {
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

    private class RouteDiscoveryState {
        public int rreqAttempts;
        public int rreqInLastSecond;
        public long nextRreqAttemptsResetInterval;

        public int ttl;

        /**
         * New discovery state, with the next timeout in one second
         */
        public RouteDiscoveryState() {
            nextRreqAttemptsResetInterval = SimulationEngine.getInstance(node.getLevel().isClientSide).getSimTime() + 1000;
        }

    }


    @Override
    public ProtocolSettings getSettings() {
        return settings;
    }

}
