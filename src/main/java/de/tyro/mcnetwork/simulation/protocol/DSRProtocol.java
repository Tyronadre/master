package de.tyro.mcnetwork.simulation.protocol;

import com.mojang.datafixers.util.Pair;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.simulation.IHudRenderer;
import de.tyro.mcnetwork.simulation.INetworkNode;
import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.SimulationEngine;
import de.tyro.mcnetwork.simulation.packet.IApplicationPacket;
import de.tyro.mcnetwork.simulation.packet.INetworkPacket;
import de.tyro.mcnetwork.simulation.packet.IProtocolPaket;
import de.tyro.mcnetwork.simulation.packet.application.DestinationUnreachablePacket;
import de.tyro.mcnetwork.simulation.packet.application.TraceRoutePacket;
import de.tyro.mcnetwork.simulation.packet.dsr.DSRRouteError;
import de.tyro.mcnetwork.simulation.packet.dsr.DSRRouteReply;
import de.tyro.mcnetwork.simulation.packet.dsr.DSRRouteRequest;
import de.tyro.mcnetwork.simulation.packet.dsr.DSRSourceRoute;
import de.tyro.mcnetwork.util.FixedFiFoQueue;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.StringJoiner;

public class DSRProtocol implements IRoutingProtocol, IHudRenderer {
    private static final Logger log = LoggerFactory.getLogger(DSRProtocol.class);

    // CONSTANTS
    private short BROADCAST_JITTER = 10;       //ms
    private final short REQUEST_TABLE_SIZE = 64;     //nodes
    private final short REQUEST_TABLE_IDS = 16;      //identifiers
    private final short MAX_REQUEST_REXMT = 16;      //retransmissions
    private short MAX_REQUEST_PERIOD = 1;     //sec
    private short REQUEST_PERIOD = 100;        //ms
    private double RENDER_SCALE = 1;

    private final RouteRequestTable routeRequestTable;
    private final RouteCache routeCache;
    private final INetworkNode node;
    private int rreqID;
    private final SimulationEngine sim;
    private final Random random = new Random();
    private final TickActions tickActions = new TickActions();
    private final ProtocolSettings settings = new ProtocolSettings();
    private final Map<IP, List<Pair<INetworkPacket, Integer>>> pendingData = new HashMap<>();


    public DSRProtocol(INetworkNode node) {
        this.node = node;
        this.routeRequestTable = new RouteRequestTable();
        routeCache = new RouteCache();
        sim = SimulationEngine.getInstance(node.getLevel().isClientSide);

        settings.registerSetting("BROADCAST_JITTER", Short.class, () -> BROADCAST_JITTER, v -> BROADCAST_JITTER = v);
        settings.registerSetting("REQUEST_TABLE_SIZE", Short.class, () -> REQUEST_TABLE_SIZE);
        settings.registerSetting("REQUEST_TABLE_IDS", Short.class, () -> REQUEST_TABLE_IDS);
        settings.registerSetting("MAX_REQUEST_REXMT", Short.class, () -> MAX_REQUEST_REXMT);
        settings.registerSetting("MAX_REQUEST_PERIOD", Short.class, () -> MAX_REQUEST_PERIOD, v -> MAX_REQUEST_PERIOD = v);
        settings.registerSetting("REQUEST_PERIOD", Short.class, () -> REQUEST_PERIOD, v -> REQUEST_PERIOD = v);
        settings.registerSetting("RENDER_SCALE", Double.class, () -> RENDER_SCALE, v -> RENDER_SCALE = v);
        settings.registerSetting("RC_ININT_STABILITY", Double.class, () -> routeCache.INIT_STABILITY, v -> routeCache.INIT_STABILITY = v);
        settings.registerSetting("RC_STABILITY_INCR_FACTOR", Double.class, () -> routeCache.STABILITY_INCR_FACTOR, v -> routeCache.STABILITY_INCR_FACTOR = v);
        settings.registerSetting("RC_STABILITY_DECR_FACTOR", Double.class, () -> routeCache.STABILITY_DECR_FACTOR, v -> routeCache.STABILITY_DECR_FACTOR = v);
        settings.registerSetting("MIN_LIFETIME", Double.class, () -> routeCache.MIN_LIFETIME, v -> routeCache.MIN_LIFETIME = v);
        settings.registerSetting("USE_EXTENDS", Double.class, () -> routeCache.USE_EXTENDS, v -> routeCache.USE_EXTENDS = v);
    }

    @Override
    public void onProtocolPacketReceived(IProtocolPaket packet) {

        //   A node forwarding or otherwise overhearing any packet SHOULD add all
        //   usable routing information from that packet to its own Route Cache.
        //   The usefulness of routing information in a packet depends on the
        //   directionality characteristics of the physical medium (Section 2), as
        //   well as on the MAC protocol being used.  Specifically, three distinct
        //   cases are possible:
        //
        //   -  Links in the network frequently are capable of operating only
        //      unidirectionally (not bidirectionally), and the MAC protocol in
        //      use in the network is capable of transmitting unicast packets over
        //      unidirectional links.
        //
        //   -  Links in the network occasionally are capable of operating only
        //      unidirectionally (not bidirectionally), but this unidirectional
        //      restriction on any link is not persistent; almost all links are
        //      physically bidirectional, and the MAC protocol in use in the
        //      network is capable of transmitting unicast packets over
        //      unidirectional links.
        //
        //   -  The MAC protocol in use in the network is not capable of
        //      transmitting unicast packets over unidirectional links; only
        //      bidirectional links can be used by the MAC protocol for
        //      transmitting unicast packets.  For example, the IEEE 802.11
        //      Distributed Coordination Function (DCF) MAC protocol [IEEE80211]
        //      is capable of transmitting a unicast packet only over a
        //      bidirectional link, since the MAC protocol requires the return of
        //      a link-level acknowledgement packet from the receiver and also
        //      optionally requires the bidirectional exchange of an RTS and CTS
        //      packet between the transmitter and receiver nodes.
        //
        //   In the first case above, for example, the source route used in a data
        //   packet, the accumulated route record in a Route Request, or the route
        //   being returned in a Route Reply SHOULD all be cached by any node in
        //   the "forward" direction.  Any node SHOULD cache this information from
        //   any such packet received, whether the packet was addressed to this
        //   node, sent to a broadcast (or multicast) MAC address, or overheard
        //   while the node's network interface is in promiscuous mode.  However,
        //   the "reverse" direction of the links identified in such packet
        //   headers SHOULD NOT be cached.
        //
        //   For example, in the situation shown below, node A is using a source
        //   route to communicate with node E:
        //
        //      +-----+     +-----+     +-----+     +-----+     +-----+
        //      |  A  |---->|  B  |---->|  C  |---->|  D  |---->|  E  |
        //      +-----+     +-----+     +-----+     +-----+     +-----+
        //
        //   As node C forwards a data packet along the route from A to E, it
        //   SHOULD add to its cache the presence of the "forward" direction links
        //   that it learns from the headers of these packets, from itself to D
        //   and from D to E.  Node C SHOULD NOT, in this case, cache the
        //   "reverse" direction of the links identified in these packet headers,
        //   from itself back to B and from B to A, since these links might be
        //   unidirectional.
        //
        //   In the second case above, in which links may occasionally operate
        //   unidirectionally, the links described above SHOULD be cached in both
        //   directions.  Furthermore, in this case, if node X overhears (e.g.,
        //   through promiscuous mode) a packet transmitted by node C that is
        //   using a source route from node A to E, node X SHOULD cache all of
        //   these links as well, also including the link from C to X over which
        //   it overheard the packet.
        //
        //   In the final case, in which the MAC protocol requires physical
        //   bidirectionality for unicast operation, links from a source route
        //   SHOULD be cached in both directions, except when the packet also
        //   contains a Route Reply, in which case only the links already
        //   traversed in this source route SHOULD be cached.  However, the links
        //   not yet traversed in this route SHOULD NOT be cached.

        //we have the final case always.
        if (packet.getNetworkFrame() != null)
            routeCache.addLink(getNode().getIP(), packet.getNetworkFrame().getFrom().getIP(), sim.getSimTime());

        switch (packet) {
            case DSRSourceRoute route -> {
                var nextAddress = route.getNextAddress();
                if (nextAddress == null) {
                    if (route.getPacket() instanceof IApplicationPacket ap) getNode().onApplicationPacketReceived(ap);
                    else if (route.getPacket() instanceof IProtocolPaket pp) onProtocolPacketReceived(pp);
                } else if (route.getPacket() instanceof TraceRoutePacket tp && packet.getNetworkFrame().getTtl() == 1) {
                    getNode().onApplicationPacketReceived(tp);
                } else {
                    if (!createRERR(route, nextAddress))
                        sim.unicast(getNode(), sim.getNode(nextAddress), packet, packet.getNetworkFrame().getTtl() - 1);
                }

                if (route.getPacket() instanceof DSRRouteError rerr) {
                    handleRERR(rerr);
                }

                routeCache.addRoute(route.getAddresses());
            }
            case DSRRouteReply rrep -> {
                handleRREP(rrep);
            }
            case DSRRouteRequest rreq -> {
                handleRREQ(rreq);
                routeCache.addRoute(rreq.getRouteToHere());
            }
            case DSRRouteError rerr -> handleRERR(rerr);
            default -> throw new RuntimeException("Unknown packet " + packet);
        }
    }

    private boolean createRERR(DSRSourceRoute erroringRoute, IP nextAddress) {
        if (sim.getNeighbors(node).stream().anyMatch(it -> it.getIP().equals(nextAddress))) return false;

        routeCache.removeLink(routeCache.getLink(node.getIP(), nextAddress));
        // 8.3.4.  Originating a Route Error
        //
        //   When a node is unable to verify reachability of a next-hop node after
        //   reaching a maximum number of retransmission attempts, it SHOULD send
        //   a Route Error to the IP Source Address of the packet.  When sending a
        //   Route Error for a packet containing either a Route Error option or an
        //   Acknowledgement option, a node SHOULD add these existing options to
        //   its Route Error, subject to the limit described below.
        //
        //   A node transmitting a Route Error MUST perform the following steps:
        //
        //   -  Create an IP packet and set the IP Protocol field to the protocol
        //      number assigned for DSR (48).  Set the Source Address field in
        //      this packet's IP header to the address of this node.
        //
        //   -  If the Salvage field in the DSR Source Route option in the packet
        //      triggering the Route Error is zero, then copy the Source Address
        //      field of the packet triggering the Route Error into the
        //      Destination Address field in the new packet's IP header;
        //
        //      otherwise, copy the Address[1] field from the DSR Source Route
        //      option of the packet triggering the Route Error into the
        //      Destination Address field in the new packet's IP header
        var rerr = new DSRRouteError(node.getIP(), erroringRoute.getAddresses().getFirst());
        //
        //   -  Insert a DSR Options header into the new packet.
        //
        //   -  Add a Route Error Option to the new packet, setting the Error Type
        //      to NODE_UNREACHABLE, the Salvage value to the Salvage value from
        //      the DSR Source Route option of the packet triggering the Route
        //      Error, and the Unreachable Node Address field to the address of
        //      the next-hop node from the original source route.  Set the Error
        //      Source Address field to this node's IP address, and the Error
        //      Destination field to the new packet's IP Destination Address.
        rerr.unreachableNodeAddress = nextAddress;
        rerr.errorSourceAddress = node.getIP();
        rerr.errorDestinationAddress = erroringRoute.getAddresses().getFirst();
        //
        //   -  If the packet triggering the Route Error contains any Route Error
        //      or Acknowledgement options, the node MAY append to its Route Error
        //      each of these options, with the following constraints:
        //
        //      o  The node MUST NOT include any Route Error option from the
        //         packet triggering the new Route Error, for which the total
        //         Salvage count (Section 6.4) of that included Route Error would
        //         be greater than MAX_SALVAGE_COUNT in the new packet.
        //
        //      o  If any Route Error option from the packet triggering the new
        //         Route Error is not included in the packet, the node MUST NOT
        //         include any following Route Error or Acknowledgement options
        //         from the packet triggering the new Route Error.
        //
        //      o  Any appended options from the packet triggering the Route Error
        //         MUST follow the new Route Error in the packet.
        //
        //      o  In appending these options to the new Route Error, the order of
        //         these options from the packet triggering the Route Error MUST
        //         be preserved.
        //
        //   -  Send the packet as described in Section 8.1.1.
        send(rerr, routeCache.getRoute(rerr.getDestinationIP()).size());
        return true;
    }

    private void handleRERR(DSRRouteError rerr) {
        // 8.3.5.  Processing a Received Route Error Option
        //   When a node receives a packet containing a Route Error option, that
        //   node MUST process the Route Error option according to the following
        //   sequence of steps:
        //
        //   -  The node MUST remove from its Route Cache the link from the node
        //      identified by the Error Source Address field to the node
        //      identified by the Unreachable Node Address field (if this link is
        //      present in its Route Cache).  If the node implements its Route
        //      Cache as a link cache, as described in Section 4.1, only this
        //      single link is removed; if the node implements its Route Cache as
        //      a path cache, however, all routes (paths) that use this link are
        //      either truncated before the link or removed completely.

        routeCache.removeLink(routeCache.getLink(rerr.errorSourceAddress, rerr.unreachableNodeAddress));

        //   -  If the option following the Route Error is an Acknowledgement or
        //      Route Error option sent by this node (that is, with
        //      Acknowledgement or Error Source Address equal to this node's
        //      address), copy the DSR options following the current Route Error
        //      into a new packet with IP Source Address equal to this node's own
        //      IP address and IP Destination Address equal to the Acknowledgement
        //      or Error Destination Address.  Transmit this packet as described
        //      in Section 8.1.1, with the Salvage count in the DSR Source Route
        //      option set to the Salvage value of the Route Error.
        //
        //   In addition, after processing the Route Error as described above, the
        //   node MAY initiate a new Route Discovery for any destination node for
        //   which it then has no route in its Route Cache as a result of
        //   processing this Route Error, if the node has indication that a route
        //   to that destination is needed.  For example, if the node has an open
        //   TCP connection to some destination node, then if the processing of
        //   this Route Error removed the only route to that destination from this
        //   node's Route Cache, then this node MAY initiate a new Route Discovery
        //   for that destination node.  Any node, however, MUST limit the rate at
        //   which it initiates new Route Discoveries for any single destination
        //   address, and any new Route Discovery initiated in this way as part of
        //   processing this Route Error MUST conform as a part of this limit.

    }

    private void handleRREQ(DSRRouteRequest rreq) {
        // 8.2.2.  Processing a Received Route Request Option
        //
        //   When a node receives a packet containing a Route Request option, that
        //   node MUST process the option according to the following sequence of
        //   steps:
        //
        //   -  If the Target Address field in the Route Request matches this
        //      node's own IP address, then the node SHOULD return a Route Reply
        //      to the initiator of this Route Request (the Source Address in the
        //      IP header of the packet), as described in Section 8.2.4.  The
        //      source route for this Reply is the sequence of hop addresses
        //
        //         initiator, Address[1], Address[2], ..., Address[n], target
        //
        //      where initiator is the address of the initiator of this Route
        //      Request, each Address[i] is an address from the Route Request, and
        //      target is the target of the Route Request (the Target Address
        //      field in the Route Request).  The value n here is the number of
        //      addresses recorded in the Route Request, or
        //      (Opt Data Len - 6) / 4.
        //
        //      The node then MUST replace the Destination Address field in the
        //      Route Request packet's IP header with the value in the Target
        //      Address field in the Route Request option, and continue processing
        //      the rest of the Route Request packet normally.  The node MUST NOT
        //      process the Route Request option further and MUST NOT retransmit
        //      the Route Request to propagate it to other nodes as part of the
        //      Route Discovery.

        if (rreq.getTargetAddress().equals(getNode().getIP())) {
            createRREP(rreq);
            return;
        }

        //
        //   -  Else, the node MUST examine the route recorded in the Route
        //      Request option (the IP Source Address field and the sequence of
        //      Address[i] fields) to determine if this node's own IP address
        //      already appears in this list of addresses.  If so, the node MUST
        //      discard the entire packet carrying the Route Request option.

        if (rreq.getAddresses().contains(getNode().getIP())) return;

        //   -  Else, if the Route Request was received through a network
        //      interface that requires physically bidirectional links for unicast
        //      transmission, the node MUST check if the Route Request was last
        //      forwarded by a node on its blacklist (Section 4.6).  If such an
        //      entry is found in the blacklist, and the state of the
        //      unidirectional link is "probable", then the Request MUST be
        //      silently discarded.

        //ignored

        //   -  Else, if the Route Request was received through a network
        //      interface that requires physically bidirectional links for unicast
        //      transmission, the node MUST check if the Route Request was last
        //      forwarded by a node on its blacklist.  If such an entry is found
        //      in the blacklist, and the state of the unidirectional link is
        //      "questionable", then the node MUST create and unicast a Route
        //      Request packet to that previous node, setting the IP Time-To-Live
        //      (TTL) to 1 to prevent the Request from being propagated.  If the
        //      node receives a Route Reply in response to the new Request, it
        //      MUST remove the blacklist entry for that node, and SHOULD continue
        //      processing.  If the node does not receive a Route Reply within
        //      some reasonable amount of time, the node MUST silently discard the
        //      Route Request packet.

        //ignored

        //   -  Else, the node MUST search its Route Request Table for an entry
        //      for the initiator of this Route Request (the IP Source Address
        //      field).  If such an entry is found in the table, the node MUST
        //      search the cache of Identification values of recently received
        //      Route Requests in that table entry, to determine if an entry is
        //      present in the cache matching the Identification value and target
        //      node address in this Route Request.  If such an (Identification,
        //      target address) entry is found in this cache in this entry in the
        //      Route Request Table, then the node MUST discard the entire packet
        //      carrying the Route Request option.

        var entry = routeRequestTable.forOrigin(rreq.getOriginatorIP());

        if (entry.isDuplicate(rreq.getTargetAddress(), rreq.getIdentificationValue())) return;


        //   -  Else, this node SHOULD further process the Route Request according
        //      to the following sequence of steps:
        //
        //      o  Add an entry for this Route Request in its cache of
        //         (Identification, target address) values of recently received
        //         Route Requests.

        entry.seen.add(Pair.of(rreq.getTargetAddress(), rreq.getIdentificationValue()));

        //      o  Conceptually create a copy of this entire packet and perform
        //         the following steps on the copy of the packet.

        var copy = rreq.copy();

        //      o  Append this node's own IP address to the list of Address[i]
        //         values in the Route Request and increase the value of the Opt
        //         Data Len field in the Route Request by 4 (the size of an IP
        //         address).  However, if the node has multiple network
        //         interfaces, this step MUST be modified by the special
        //         processing specified in Section 8.4.

        copy.addAddress(getNode().getIP());

        //      o  This node SHOULD search its own Route Cache for a route (from
        //         itself, as if it were the source of a packet) to the target of
        //         this Route Request.  If such a route is found in its Route
        //         Cache, then this node SHOULD follow the procedure outlined in
        //         Section 8.2.3 to return a "cached Route Reply" to the initiator
        //         of this Route Request, if permitted by the restrictions
        //         specified there.

        if (handleCachedRREP(rreq)) return;

        //      o  If the node does not return a cached Route Reply, then this
        //         node SHOULD transmit this copy of the packet as a link-layer
        //         broadcast, with a short jitter delay before the broadcast is
        //         sent.  The jitter period SHOULD be chosen as a random period,
        //         uniformly distributed between 0 and BroadcastJitter.

        tickActions.add(sim.getSimTime() + random.nextInt(BROADCAST_JITTER), () -> sim.broadcast(getNode(), copy, rreq.getNetworkFrame().getTtl() - 1));
    }

    private boolean handleCachedRREP(DSRRouteRequest rreq) {
        //8.2.3.  Generating a Route Reply Using the Route Cache
        //
        //   As described in Section 3.3.2, it is possible for a node processing a
        //   received Route Request to avoid propagating the Route Request further
        //   toward the target of the Request, if this node has in its Route Cache
        //   a route from itself to this target.  Such a Route Reply generated by
        //   a node from its own cached route to the target of a Route Request is
        //   called a "cached Route Reply", and this mechanism can greatly reduce
        //   the overall overhead of Route Discovery on the network by reducing
        //   the flood of Route Requests.  The general processing of a received
        //   Route Request is described in Section 8.2.2; this section specifies
        //   the additional requirements that MUST be met before a cached Route
        //   Reply may be generated and returned and specifies the procedure for
        //   returning such a cached Route Reply.
        //
        //   While processing a received Route Request, for a node to possibly
        //   return a cached Route Reply, it MUST have in its Route Cache a route
        //   from itself to the target of this Route Request.  However, before
        //   generating a cached Route Reply for this Route Request, the node MUST
        //   verify that there are no duplicate addresses listed in the route
        //   accumulated in the Route Request together with the route from this
        //   node's Route Cache.  Specifically, there MUST be no duplicates among
        //   the following addresses:
        //
        //   -  The IP Source Address of the packet containing the Route Request,
        //
        //   -  The Address[i] fields in the Route Request, and
        //
        //   -  The nodes listed in the route obtained from this node's Route
        //      Cache, excluding the address of this node itself (this node itself
        //      is the common point between the route accumulated in the Route
        //      Request and the route obtained from the Route Cache).
        //
        //   If any duplicates exist among these addresses, then the node MUST NOT
        //   send a cached Route Reply using this route from the Route Cache (it
        //   is possible that this node has another route in its Route Cache for
        //   which the above restriction on duplicate addresses is met, allowing
        //   the node to send a cached Route Reply based on that cached route,
        //   instead).  The node SHOULD continue to process the Route Request as
        //   described in Section 8.2.2 if it does not send a cached Route Reply.
        //
        //   If the Route Request and the route from the Route Cache meet the
        //   restriction above, then the node SHOULD construct and return a cached
        //   Route Reply as follows:
        //
        //   -  The source route for this Route Reply is the sequence of hop
        //      addresses
        //
        //         initiator, Address[1], Address[2], ..., Address[n], c-route
        //
        //      where initiator is the address of the initiator of this Route
        //      Request, each Address[i] is an address from the Route Request, and
        //      c-route is the sequence of hop addresses in the source route to
        //      this target node, obtained from the node's Route Cache.  In
        //      appending this cached route to the source route for the reply, the
        //      address of this node itself MUST be excluded, since it is already
        //      listed as Address[n].
        //
        //   -  Send a Route Reply to the initiator of the Route Request, using
        //      the procedure defined in Section 8.2.4.  The initiator of the
        //      Route Request is indicated in the Source Address field in the
        //      packet's IP header.
        //
        //   Before sending the cached Route Reply, however, the node MAY delay
        //   the Reply in order to help prevent a possible Route Reply "storm", as
        //   described in Section 8.2.5.
        //
        //   If the node returns a cached Route Reply as described above, then the
        //   node MUST NOT propagate the Route Request further (i.e., the node
        //   MUST NOT rebroadcast the Route Request).  In this case, instead, if
        //   the packet contains no other DSR options and contains no payload
        //   after the DSR Options header (e.g., the Route Request is not
        //   piggybacked on a TCP or UDP packet), then the node SHOULD simply
        //   discard the packet.  Otherwise (if the packet contains other DSR
        //   options or contains any payload after the DSR Options header), the
        //   node SHOULD forward the packet along the cached route to the target
        //   of the Route Request.  Specifically, if the node does so, it MUST use
        //   the following steps:
        //
        //   -  Copy the Target Address from the Route Request option in the DSR
        //      Options header to the Destination Address field in the packet's IP
        //      header.
        //
        //   -  Remove the Route Request option from the DSR Options header in the
        //      packet, and add a DSR Source Route option to the packet's DSR
        //      Options header.
        //
        //   -  In the DSR Source Route option, set the Address[i] fields to
        //      represent the source route found in this node's Route Cache to the
        //      original target of the Route Discovery (the new IP Destination
        //      Address of the packet).  Specifically, the node copies the hop
        //      addresses of the source route into sequential Address[i] fields in
        //      the DSR Source Route option, for i = 1, 2, ..., n.  Address[1],
        //      here, is the address of this node itself (the first address in the
        //      source route found from this node to the original target of the
        //      Route Discovery).  The value n, here, is the number of hop
        //      addresses in this source route, excluding the destination of the
        //      packet (which is instead already represented in the Destination
        //      Address field in the packet's IP header).
        //
        //   -  Initialize the Segments Left field in the DSR Source Route option
        //      to n as defined above.
        //
        //   -  The First Hop External (F) bit in the DSR Source Route option MUST
        //      be set to 0.
        //
        //   -  The Last Hop External (L) bit in the DSR Source Route option is
        //      copied from the External bit flagging the last hop in the source
        //      route for the packet, as indicated in the Route Cache.
        //
        //   -  The Salvage field in the DSR Source Route option MUST be
        //      initialized to some nonzero value; the particular nonzero value
        //      used SHOULD be MAX_SALVAGE_COUNT.  By initializing this field to a
        //      nonzero value, nodes forwarding or overhearing this packet will
        //      not consider a link to exist between the IP Source Address of the
        //      packet and the Address[1] address in the DSR Source Route option
        //      (e.g., they will not attempt to add this to their Route Cache as a
        //      link).  By choosing MAX_SALVAGE_COUNT as the nonzero value to
        //      which the node initializes this field, nodes furthermore will not
        //      attempt to salvage this packet.
        //
        //   -  Transmit the packet to the next-hop node on the new source route
        //      in the packet, using the forwarding procedure described in Section
        //      8.1.5.
        return false;
    }

    private void createRREP(DSRRouteRequest rreq) {
        //8.2.4.  Originating a Route Reply
        //
        //   A node originates a Route Reply in order to reply to a received and
        //   processed Route Request, according to the procedures described in
        //   Sections 8.2.2 and 8.2.3.  The Route Reply is returned in a Route
        //   Reply option (Section 6.3).  The Route Reply option MAY be returned
        //   to the initiator of the Route Request in a separate IP packet, used
        //   only to carry this Route Reply option, or it MAY be included in any
        //   other IP packet being sent to this address.
        //
        //   The Route Reply option MUST be included in a DSR Options header in
        //   the packet returned to the initiator.  To initialize the Route Reply
        //   option, the node performs the following sequence of steps:
        //
        //   -  The Option Type in the option MUST be set to the value 3.
        //
        //   -  The Opt Data Len field in the option MUST be set to the value
        //      (n * 4) + 3, where n is the number of addresses in the source
        //      route being returned (excluding the Route Discovery initiator
        //      node's address).
        //
        //   -  If this node is the target of the Route Request, the Last Hop
        //      External (L) bit in the option MUST be initialized to 0.
        //
        //   -  The Reserved field in the option MUST be initialized to 0.
        //
        //   -  The Route Request Identifier MUST be initialized to the Identifier
        //      field of the Route Request to which this Route Reply is sent in
        //      response.
        //
        //   -  The sequence of hop addresses in the source route are copied into
        //      the Address[i] fields of the option.  Address[1] MUST be set to
        //      the first-hop address of the route after the initiator of the
        //      Route Discovery, Address[n] MUST be set to the last-hop address of
        //      the source route (the address of the target node), and each other
        //      Address[i] MUST be set to the next address in sequence in the
        //      source route being returned.
        //
        //   The Destination Address field in the IP header of the packet carrying
        //   the Route Reply option MUST be set to the address of the initiator of
        //   the Route Discovery (i.e., for a Route Reply being returned in
        //   response to some Route Request, the IP Source Address of the Route
        //   Request).

        var route = new ArrayList<>(rreq.getAddresses());
        route.add(node.getIP());
        Collections.reverse(route);

        var rrep = new DSRRouteReply(node.getIP(), rreq.getOriginatorIP());
        rrep.setAddresses(route);
        rrep.setLastHopExternal(false);
        rrep.setIdentifificationValue(rreq.getIdentificationValue());


        //   After creating and initializing the Route Reply option and the IP
        //   packet containing it, send the Route Reply.  In sending the Route
        //   Reply from this node (but not from nodes forwarding the Route Reply),
        //   this node SHOULD delay the Reply by a small jitter period chosen
        //   randomly between 0 and BroadcastJitter.

        tickActions.add(sim.getSimTime() + random.nextInt(BROADCAST_JITTER), () -> sim.unicast(getNode(), sim.getNode(rreq.getNetworkFrame().getFrom().getIP()), rrep, route.size()));

        //   When returning any Route Reply in the case in which the MAC protocol
        //   in use in the network is not capable of transmitting unicast packets
        //   over unidirectional links, the source route used for routing the
        //   Route Reply packet MUST be obtained by reversing the sequence of hops
        //   in the Route Request packet (the source route that is then returned
        //   in the Route Reply).  This restriction on returning a Route Reply
        //   enables the Route Reply to test this sequence of hops for
        //   bidirectionality, preventing the Route Reply from being received by
        //   the initiator of the Route Discovery unless each of the hops over
        //   which the Route Reply is returned (and thus each of the hops in the
        //   source route being returned in the Reply) is bidirectional.
        //
        //   If sending a Route Reply to the initiator of the Route Request
        //   requires performing a Route Discovery, the Route Reply option MUST be
        //   piggybacked on the packet that contains the Route Request.  This
        //   piggybacking prevents a recursive dependency wherein the target of
        //   the new Route Request (which was itself the initiator of the original
        //   Route Request) must do another Route Request in order to return its
        //   Route Reply.
        //
        //   If sending the Route Reply to the initiator of the Route Request does
        //   not require performing a Route Discovery, a node SHOULD send a
        //   unicast Route Reply in response to every Route Request it receives
        //   for which it is the target node.

        //we ignore all of this for now

        //8.2.5.  Preventing Route Reply Storms
        //
        //   The ability for nodes to reply to a Route Request based on
        //   information in their Route Caches, as described in Sections 3.3.2 and
        //   8.2.3, could result in a possible Route Reply "storm" in some cases.
        //   In particular, if a node broadcasts a Route Request for a target node
        //
        //
        //
        //Johnson, et al.               Experimental                     [Page 72]
        //
        //RFC 4728          The Dynamic Source Routing Protocol      February 2007
        //
        //
        //   for which the node's neighbors have a route in their Route Caches,
        //   each neighbor may attempt to send a Route Reply, thereby wasting
        //   bandwidth and possibly increasing the number of network collisions in
        //   the area.
        //
        //   For example, the figure below shows a situation in which nodes B, C,
        //   D, E, and F all receive A's Route Request for target G, and each has
        //   the indicated route cached for this target:
        //
        //                +-----+                 +-----+
        //                |  D  |<               >|  C  |
        //                +-----+ \             / +-----+
        //      Cache: C - B - G   \           /  Cache: B - G
        //                          \ +-----+ /
        //                           -|  A  |-
        //                            +-----+\     +-----+     +-----+
        //                             |   |  \--->|  B  |     |  G  |
        //                            /     \      +-----+     +-----+
        //                           /       \     Cache: G
        //                          v         v
        //                    +-----+         +-----+
        //                    |  E  |         |  F  |
        //                    +-----+         +-----+
        //               Cache: F - B - G     Cache: B - G
        //
        //   Normally, each of these nodes would attempt to reply from its own
        //   Route Cache, and they would thus all send their Route Replies at
        //   about the same time, since they all received the broadcast Route
        //   Request at about the same time.  Such simultaneous Route Replies from
        //   different nodes all receiving the Route Request may cause local
        //   congestion in the wireless network and may create packet collisions
        //   among some or all of these Replies if the MAC protocol in use does
        //   not provide sufficient collision avoidance for these packets.  In
        //   addition, it will often be the case that the different replies will
        //   indicate routes of different lengths, as shown in this example.
        //
        //   In order to reduce these effects, if a node can put its network
        //   interface into promiscuous receive mode, it MAY delay sending its own
        //   Route Reply for a short period, while listening to see if the
        //   initiating node begins using a shorter route first.  Specifically,
        //   this node MAY delay sending its own Route Reply for a random period
        //
        //      d = H * (h - 1 + r)
        //
        //   where h is the length in number of network hops for the route to be
        //   returned in this node's Route Reply, r is a random floating point
        //   number between 0 and 1, and H is a small constant delay (at least
        //   twice the maximum wireless link propagation delay) to be introduced
        //   per hop.  This delay effectively randomizes the time at which each
        //   node sends its Route Reply, with all nodes sending Route Replies
        //   giving routes of length less than h sending their Replies before this
        //   node, and all nodes sending Route Replies giving routes of length
        //   greater than h send their Replies after this node.
        //
        //   Within the delay period, this node promiscuously receives all
        //   packets, looking for data packets from the initiator of this Route
        //   Discovery destined for the target of the Route Discovery.  If such a
        //   data packet received by this node during the delay period uses a
        //   source route of length less than or equal to h, this node may infer
        //   that the initiator of the Route Discovery has already received a
        //   Route Reply giving an equally good or better route.  In this case,
        //   this node SHOULD cancel its delay timer and SHOULD NOT send its Route
        //   Reply for this Route Discovery.
    }

    private void handleRREP(DSRRouteReply rrep) {
        var thisIP = node.getIP();

        //save the route
        routeCache.addRoute(rrep.getRouteToHere(thisIP));

        // this is not the destination.
        if (!rrep.getDestinationIP().equals(getNode().getIP())) {

            var nextIP = rrep.getNextAddress(thisIP);

            sim.unicast(getNode(), sim.getNode(nextIP), rrep, rrep.getNetworkFrame().getTtl() - 1);
            return;
        }

        //we are the destination. check for any packets that are in the buffer and send them.
        var pending = pendingData.remove(rrep.getOriginatorIP());
        if (pending == null) return;
        for (Pair<INetworkPacket, Integer> pair : pending) send(pair.getFirst(), pair.getSecond());
    }

    @Override
    public void send(INetworkPacket packet, int ttl) {

        if (node.getIP().equals(packet.getDestinationIP())) {
            if (packet instanceof IApplicationPacket a) {
                node.onApplicationPacketReceived(a);
                return;
            } else {
                throw new IllegalArgumentException("Trying to send a packet of type " + packet.getClass().getSimpleName() + " to its own node");
            }
        }

        if (packet.getDestinationIP().equals(IP.BROADCAST)) {
            sim.broadcast(node, packet, ttl);
            return;
        }

        if (hasRoute(packet.getDestinationIP())) {
            var route = routeCache.getRoute(packet.getDestinationIP());
            var dsrRoutePacket = new DSRSourceRoute(packet.getOriginatorIP(), packet.getDestinationIP(), route, false, false, packet);
            var nextAddress = dsrRoutePacket.getNextAddress();

            if (!createRERR(dsrRoutePacket, nextAddress))
                sim.unicast(getNode(), sim.getNode(nextAddress), dsrRoutePacket, ttl);
        }

        pendingData.computeIfAbsent(packet.getDestinationIP(), k -> new ArrayList<>()).add(new Pair<>(packet, ttl));
        discoverRoute(packet.getDestinationIP());
    }

    @Override
    public void discoverRoute(IP destinationIp) {
        if (hasRoute(destinationIp)) {
            routeRequestTable.removeForDestination(destinationIp);
            return;
        }

        //   Route Discovery is the mechanism by which a node S wishing to send a
        //   packet to a destination node D obtains a source route to D.  Route
        //   Discovery SHOULD be used only when S attempts to send a packet to D
        //   and does not already know a route to D.  The node initiating a Route
        //   Discovery is known as the "initiator" of the Route Discovery, and the
        //   destination node for which the Route Discovery is initiated is known
        //   as the "target" of the Route Discovery.
        //
        //   Route Discovery operates entirely on demand; a node initiates Route
        //   Discovery based on its own origination of new packets for some
        //   destination address to which it does not currently know a route.
        //   Route Discovery does not depend on any periodic or background
        //   exchange of routing information or neighbor node detection at any
        //   layer in the network protocol stack at any node.
        //
        //   The Route Discovery procedure utilizes two types of messages, a Route
        //   Request (Section 6.2) and a Route Reply (Section 6.3), to actively
        //   search the ad hoc network for a route to the desired target
        //   destination.  These DSR messages MAY be carried in any type of IP
        //   packet, through use of the DSR Options header as described in Section
        //   6.
        //
        //   Except as discussed in Section 8.3.5, a Route Discovery for a
        //   destination address SHOULD NOT be initiated unless the initiating
        //   node has a packet in its Send Buffer requiring delivery to that
        //   destination.  A Route Discovery for a given target node MUST NOT be
        //   initiated unless permitted by the rate-limiting information contained
        //   in the Route Request Table.  After each Route Discovery attempt, the
        //   interval between successive Route Discoveries for this target SHOULD
        //   be doubled, up to a maximum of MaxRequestPeriod, until a valid Route
        //   Reply is received for this target.

        // 8.2.1.  Originating a Route Request
        //
        //   A node initiating a Route Discovery for some target creates and
        //   initializes a Route Request option in a DSR Options header in some IP
        //   packet.  This MAY be a separate IP packet, used only to carry this
        //   Route Request option, or the node MAY include the Route Request
        //   option in some existing packet that it needs to send to the target
        //   node (e.g., the IP packet originated by this node that caused the
        //   node to attempt Route Discovery for the destination address of the
        //   packet).  The Route Request option MUST be included in a DSR Options
        //   header in the packet.  To initialize the Route Request option, the
        //   node performs the following sequence of steps:
        //
        //   -  The Option Type in the option MUST be set to the value 2.
        //
        //   -  The Opt Data Len field in the option MUST be set to the value 6.
        //      The total size of the Route Request option, when initiated, is 8
        //      octets; the Opt Data Len field excludes the size of the Option
        //      Type and Opt Data Len fields themselves.
        //
        //   -  The Identification field in the option MUST be set to a new value,
        //      different from that used for other Route Requests recently
        //      initiated by this node for this same target address.  For example,
        //      each node MAY maintain a single counter value for generating a new
        //      Identification value for each Route Request it initiates.
        //
        //   -  The Target Address field in the option MUST be set to the IP
        //      address that is the target of this Route Discovery.
        //
        //   The Source Address in the IP header of this packet MUST be the node's
        //   own IP address.  The Destination Address in the IP header of this
        //   packet MUST be the IP "limited broadcast" address (255.255.255.255).

        var id = rreqID++;

        var rreq = new DSRRouteRequest(getNode().getIP(), IP.BROADCAST, id);
        rreq.setTargetAddress(destinationIp);

        var rreqState = routeRequestTable.forDestination(destinationIp);
        if (rreqState.numberOfRREQ > MAX_REQUEST_REXMT) {
            pendingData.remove(destinationIp);
            node.onApplicationPacketReceived(new DestinationUnreachablePacket(node.getIP(), destinationIp));
            routeRequestTable.removeForDestination(destinationIp);
            return;
        }

        var now = sim.getSimTime();

        //   A node MUST maintain, in its Route Request Table, information about
        //   Route Requests that it initiates.  When initiating a new Route
        //   Request, the node MUST use the information recorded in the Route
        //   Request Table entry for the target of that Route Request, and it MUST
        //   update that information in the table entry for use in the next Route
        //   Request initiated for this target.  In particular:
        //
        //   -  The Route Request Table entry for a target node records the Time-
        //      to-Live (TTL) field used in the IP header of the Route Request for
        //      the last Route Discovery initiated by this node for that target
        //      node.  This value allows the node to implement a variety of
        //      algorithms for controlling the spread of its Route Request on each
        //      Route Discovery initiated for a target.  As examples, two possible
        //      algorithms for this use of the TTL field are described in Section
        //      3.3.3.

        var ttl = rreqState.ttl;
        if (ttl == 0) ttl = 1;
        else ttl *= 2;

        rreqState.setTTL(ttl);

        //   -  The Route Request Table entry for a target node records the number
        //      of consecutive Route Requests initiated for this target since
        //      receiving a valid Route Reply giving a route to that target node,
        //      and the remaining amount of time before which this node MAY next
        //      attempt at a Route Discovery for that target node.

        rreqState.setNumberOfRREQ(rreqState.getNumberOfRREQ() + 1);

        //      A node MUST use these values to implement a back-off algorithm to
        //      limit the rate at which this node initiates new Route Discoveries
        //      for the same target address.  In particular, until a valid Route
        //      Reply is received for this target node address, the timeout
        //      between consecutive Route Discovery initiations for this target
        //      node with the same hop limit SHOULD increase by doubling the
        //      timeout value on each new initiation.
        //
        //   The behavior of a node processing a packet containing DSR Options
        //   header with both a DSR Source Route option and a Route Request option
        //   is unspecified.  Packets SHOULD NOT contain both a DSR Source Route
        //   option and a Route Request option.
        //
        //   Packets containing a Route Request option SHOULD NOT include an
        //   Acknowledgement Request option, SHOULD NOT expect link-layer
        //   acknowledgement or passive acknowledgement, and SHOULD NOT be
        //   retransmitted.  The retransmission of packets containing a Route
        //   Request option is controlled solely by the logic described in this
        //   section.


        sim.broadcast(getNode(), rreq, ttl);

        rreqState.nextTry = now + (long) (Math.min(MAX_REQUEST_PERIOD * 1000, REQUEST_PERIOD * Math.pow(2, rreqState.numberOfRREQ)));

    }

    @Override
    public void simTick() {
        tickActions.execute(sim.getSimTime());
        for (var entry : routeRequestTable.sendEntries.entrySet()) {
            if (entry.getValue().nextTry < sim.getSimTime()) discoverRoute(entry.getKey());
        }
    }

    @Override
    public boolean hasRoute(IP destination) {
        return routeCache.hasRoute(destination);
    }

    @Override
    public INetworkNode getNode() {
        return node;
    }

    @Override
    public void render(RenderUtil renderer) {
        var pose = renderer.getPoseStack();
        var font = renderer.getFont();
        var width = getRenderSize(renderer.getFont()).x;
        renderer.setWidth(width);

        int y = 0;
        pose.pushPose();
        pose.scale(0.5f, 0.5f, 0.5f);
        width *= 2;

        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "Pending", width, y);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, String.valueOf(pendingData.size()), width, y);
        y += 10;

        var receivedEntries = String.valueOf(routeRequestTable.receivedEntries.size());
        var sendEntries = String.valueOf(routeRequestTable.sendEntries.size());
        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "RREQ Cache (Out|In)", width, y);
        renderer.drawString(sendEntries, RenderUtil.Color.MAGENTA.value, width / 2 - 5 - font.width(receivedEntries) - font.width(sendEntries), y, false);
        renderer.drawString(receivedEntries, RenderUtil.Color.YELLOW.value, width / 2 - font.width(receivedEntries), y, false);
        y += 5;

        pose.popPose();

        pose.translate(0, y, 0);
        routeCache.render(renderer);
    }

    @Override
    public Vec2 getRenderSize(Font font) {
        var thisSize = new Vec2(100, 25);
        var routeCacheSize = routeCache.getRenderSize(font);
        return new Vec2(Math.max(thisSize.x, routeCacheSize.x), thisSize.y + routeCacheSize.y);
    }

    /**
     * Indexed by SourceIP of the route Request
     */
    private class RouteRequestTable {
        private final Map<IP, RouteRequestTableSendEntry> sendEntries = new HashMap<>();
        private final Map<IP, RouteRequestTableReceivedEntry> receivedEntries = new LinkedHashMap<>(REQUEST_TABLE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<IP, RouteRequestTableReceivedEntry> eldest) {
                return size() > REQUEST_TABLE_SIZE;
            }
        };

        public RouteRequestTableSendEntry forDestination(IP destinationIP) {
            return sendEntries.computeIfAbsent(destinationIP, k -> new RouteRequestTableSendEntry());
        }

        public RouteRequestTableReceivedEntry forOrigin(IP originIP) {
            return receivedEntries.computeIfAbsent(originIP, k -> new RouteRequestTableReceivedEntry());
        }

        public void removeForDestination(IP destinationIp) {
            sendEntries.remove(destinationIp);
        }

        @Override
        public String toString() {
            return "Send: " + sendEntries + "\nReceived: " + receivedEntries;
        }

        private static class RouteRequestTableSendEntry {
            public long nextTry;
            private int ttl;
            private int numberOfRREQ;

            public void setTTL(int ttl) {
                this.ttl = ttl;
            }

            public int getNumberOfRREQ() {
                return this.numberOfRREQ;
            }

            public void setNumberOfRREQ(int numberOfRREQ) {
                this.numberOfRREQ = numberOfRREQ;
            }

            @Override
            public String toString() {
                return "nextTry: " + nextTry + ", ttl: " + ttl + ", numberOfRREQ: " + numberOfRREQ;
            }
        }

        private class RouteRequestTableReceivedEntry {
            private final FixedFiFoQueue<Pair<IP, Integer>> seen = new FixedFiFoQueue<>(REQUEST_TABLE_IDS);

            public boolean isDuplicate(IP target, int id) {
                return seen.stream().anyMatch(it -> it != null && it.getFirst().equals(target) && it.getSecond().equals(id));
            }

            @Override
            public String toString() {
                return seen.toString();
            }
        }
    }


    //4.1.  Route Cache
    //
    //   Each node implementing DSR MUST maintain a Route Cache, containing
    //   routing information needed by the node.  A node adds information to
    //   its Route Cache as it learns of new links between nodes in the ad hoc
    //   network; for example, a node may learn of new links when it receives
    //   a packet carrying a Route Request, Route Reply, or DSR source route.
    //   Likewise, a node removes information from its Route Cache as it
    //   learns that existing links in the ad hoc network have broken.  For
    //   example, a node may learn of a broken link when it receives a packet
    //   carrying a Route Error or through the link-layer retransmission
    //   mechanism reporting a failure in forwarding a packet to its next-hop
    //   destination.
    //
    //   Anytime a node adds new information to its Route Cache, the node
    //   SHOULD check each packet in its own Send Buffer (Section 4.2) to
    //   determine whether a route to that packet's IP Destination Address now
    //   exists in the node's Route Cache (including the information just
    //   added to the Cache).  If so, the packet SHOULD then be sent using
    //   that route and removed from the Send Buffer.
    //
    //   It is possible to interface a DSR network with other networks,
    //   external to this DSR network.  Such external networks may, for
    //   example, be the Internet or may be other ad hoc networks routed with
    //   a routing protocol other than DSR.  Such external networks may also
    //   be other DSR networks that are treated as external networks in order
    //   to improve scalability.  The complete handling of such external
    //   networks is beyond the scope of this document.  However, this
    //   document specifies a minimal set of requirements and features
    //   necessary to allow nodes only implementing this specification to
    //   interoperate correctly with nodes implementing interfaces to such
    //   external networks.  This minimal set of requirements and features
    //   involve the First Hop External (F) and Last Hop External (L) bits in
    //   a DSR Source Route option (Section 6.7) and a Route Reply option
    //   (Section 6.3) in a packet's DSR Options header (Section 6).  These
    //   requirements also include the addition of an External flag bit
    //   tagging each link in the Route Cache, copied from the First Hop
    //   External (F) and Last Hop External (L) bits in the DSR Source Route
    //   option or Route Reply option from which this link was learned.
    //
    //   The Route Cache SHOULD support storing more than one route to each
    //   destination.  In searching the Route Cache for a route to some
    //   destination node, the Route Cache is searched by destination node
    //   address.  The following properties describe this searching function
    //   on a Route Cache:
    //
    //   -  Each implementation of DSR at any node MAY choose any appropriate
    //      strategy and algorithm for searching its Route Cache and selecting
    //      a "best" route to the destination from among those found.  For
    //      example, a node MAY choose to select the shortest route to the
    //      destination (the shortest sequence of hops), or it MAY use an
    //      alternate metric to select the route from the Cache.
    //
    //   -  However, if there are multiple cached routes to a destination, the
    //      selection of routes when searching the Route Cache SHOULD prefer
    //      routes that do not have the External flag set on any link.  This
    //      preference will select routes that lead directly to the target
    //      node over routes that attempt to reach the target via any external
    //      networks connected to the DSR ad hoc network.
    //
    //   -  In addition, any route selected when searching the Route Cache
    //      MUST NOT have the External bit set for any links other than
    //      possibly the first link, the last link, or both; the External bit
    //      MUST NOT be set for any intermediate hops in the route selected.
    //
    //   An implementation of a Route Cache MAY provide a fixed capacity for
    //   the cache, or the cache size MAY be variable.  The following
    //   properties describe the management of available space within a node's
    //   Route Cache:
    //
    //   -  Each implementation of DSR at each node MAY choose any appropriate
    //      policy for managing the entries in its Route Cache, such as when
    //      limited cache capacity requires a choice of which entries to
    //      retain in the Cache.  For example, a node MAY chose a "least
    //      recently used" (LRU) cache replacement policy, in which the entry
    //      last used longest ago is discarded from the cache if a decision
    //      needs to be made to allow space in the cache for some new entry
    //      being added.
    //
    //   -  However, the Route Cache replacement policy SHOULD allow routes to
    //      be categorized based upon "preference", where routes with a higher
    //      preferences are less likely to be removed from the cache.  For
    //      example, a node could prefer routes for which it initiated a Route
    //      Discovery over routes that it learned as the result of promiscuous
    //      snooping on other packets.  In particular, a node SHOULD prefer
    //      routes that it is presently using over those that it is not.
    //
    //   Any suitable data structure organization, consistent with this
    //   specification, MAY be used to implement the Route Cache in any node.
    //   For example, the following two types of organization are possible:
    //
    //   -  In DSR, the route returned in each Route Reply that is received by
    //      the initiator of a Route Discovery (or that is learned from the
    //      header of overhead packets, as described in Section 8.1.4)
    //      represents a complete path (a sequence of links) leading to the
    //      destination node.  By caching each of these paths separately, a
    //      "path cache" organization for the Route Cache can be formed.  A
    //      path cache is very simple to implement and easily guarantees that
    //      all routes are loop-free, since each individual route from a Route
    //      Reply or Route Request or used in a packet is loop-free.  To
    //      search for a route in a path cache data structure, the sending
    //      node can simply search its Route Cache for any path (or prefix of
    //      a path) that leads to the intended destination node.
    //
    //      This type of organization for the Route Cache in DSR has been
    //      extensively studied through simulation [BROCH98, HU00,
    //      JOHANSSON99, MALTZ99a] and through implementation of DSR in a
    //      mobile outdoor testbed under significant workload [MALTZ99b,
    //      MALTZ00, MALTZ01].
    //
    //   -  Alternatively, a "link cache" organization could be used for the
    //      Route Cache, in which each individual link (hop) in the routes
    //      returned in Route Reply packets (or otherwise learned from the
    //      header of overhead packets) is added to a unified graph data
    //      structure of this node's current view of the network topology.  To
    //      search for a route in link cache, the sending node must use a more
    //      complex graph search algorithm, such as the well-known Dijkstra's
    //      shortest-path algorithm, to find the current best path through the
    //      graph to the destination node.  Such an algorithm is more
    //      difficult to implement and may require significantly more CPU time
    //      to execute.
    //      However, a link cache organization is more powerful than a path
    //      cache organization, in its ability to effectively utilize all of
    //      the potential information that a node might learn about the state
    //      of the network.  In particular, links learned from different Route
    //      Discoveries or from the header of any overheard packets can be
    //      merged together to form new routes in the network, but this is not
    //      possible in a path cache due to the separation of each individual
    //      path in the cache.
    //
    //      This type of organization for the Route Cache in DSR, including
    //      the effect of a range of implementation choices, has been studied
    //      through detailed simulation [HU00].
    //
    //   The choice of data structure organization to use for the Route Cache
    //   in any DSR implementation is a local matter for each node and affects
    //   only performance; any reasonable choice of organization for the Route
    //   Cache does not affect either correctness or interoperability.
    //
    //   Each entry in the Route Cache SHOULD have a timeout associated with
    //   it, to allow that entry to be deleted if not used within some time.
    //   The particular choice of algorithm and data structure used to
    //   implement the Route Cache SHOULD be considered in choosing the
    //   timeout for entries in the Route Cache.  The configuration variable
    //   RouteCacheTimeout defined in Section 9 specifies the timeout to be
    //   applied to entries in the Route Cache, although it is also possible
    //   to instead use an adaptive policy in choosing timeout values rather
    //   than using a single timeout setting for all entries.  For example,
    //   the Link-MaxLife cache design (below) uses an adaptive timeout
    //   algorithm and does not use the RouteCacheTimeout configuration
    //   variable.
    //
    //   As guidance to implementers, Appendix A describes a type of link
    //   cache known as "Link-MaxLife" that has been shown to outperform other
    //   types of link caches and path caches studied in detailed simulation
    //   [HU00].  Link-MaxLife is an adaptive link cache in which each link in
    //   the cache has a timeout that is determined dynamically by the caching
    //   node according to its observed past behavior of the two nodes at the
    //   ends of the link.  In addition, when selecting a route for a packet
    //   being sent to some destination, among cached routes of equal length
    //   (number of hops) to that destination, Link-MaxLife selects the route
    //   with the longest expected lifetime (highest minimum timeout of any
    //   link in the route).  Use of the Link-MaxLife design for the Route
    //   Cache is recommended in implementations of DSR.
    private class RouteCache implements IHudRenderer {
        //   As guidance to implementers of DSR, the description below outlines
        //   the operation of a possible implementation of a Route Cache for DSR
        //   that has been shown to outperform other caches studied in detailed
        //   simulations.  Use of this design for the Route Cache is recommended
        //   in implementations of DSR.
        //
        //   This cache, called "Link-MaxLife" [HU00], is a link cache, in that
        //   each individual link (hop) in the routes returned in Route Reply
        //   packets (or otherwise learned from the header of overhead packets) is
        //   added to a unified graph data structure of this node's current view
        //   of the network topology, as described in Section 4.1.  To search for
        //   a route in this cache to some destination node, the sending node uses
        //   a graph search algorithm, such as the well-known Dijkstra's
        //   shortest-path algorithm, to find the current best path through the
        //   graph to the destination node.
        //
        //   The Link-MaxLife form of link cache is adaptive in that each link in
        //   the cache has a timeout that is determined dynamically by the caching
        //   node according to its observed past behavior of the two nodes at the
        //   ends of the link; in addition, when selecting a route for a packet
        //   being sent to some destination, among cached routes of equal length
        //   (number of hops) to that destination, Link-MaxLife selects the route
        //   with the longest expected lifetime (highest minimum timeout of any
        //   link in the route).
        //
        //   Specifically, in Link-MaxLife, a link's timeout in the Route Cache is
        //   chosen according to a "Stability Table" maintained by the caching
        //   node.  Each entry in a node's Stability Table records the address of
        //   another node and a factor representing the perceived "stability" of
        //   this node.  The stability of each other node in a node's Stability
        //   Table is initialized to InitStability.  When a link from the Route
        //   Cache is used in routing a packet originated or salvaged by that
        //   node, the stability metric for each of the two endpoint nodes of that
        //   link is incremented by the amount of time since that link was last
        //   used, multiplied by StabilityIncrFactor (StabilityIncrFactor >= 1);
        //   when a link is observed to break and the link is thus removed from
        //   the Route Cache, the stability metric for each of the two endpoint
        //   nodes of that link is multiplied by StabilityDecrFactor
        //   (StabilityDecrFactor < 1).
        //
        //   When a node adds a new link to its Route Cache, the node assigns a
        //   lifetime for that link in the Cache equal to the stability of the
        //   less "stable" of the two endpoint nodes for the link, except that a
        //   link is not allowed to be given a lifetime less than MinLifetime.
        //   When a link is used in a route chosen for a packet originated or
        //   salvaged by this node, the link's lifetime is set to be at least
        //   UseExtends into the future; if the lifetime of that link in the Route
        //   Cache is already further into the future, the lifetime remains
        //   unchanged.
        //
        //   When a node using Link-MaxLife selects a route from its Route Cache
        //   for a packet being originated or salvaged by this node, it selects
        //   the shortest-length route that has the longest expected lifetime
        //   (highest minimum timeout of any link in the route), as opposed to
        //   simply selecting an arbitrary route of shortest length.
        //
        //   The following configuration variables are used in the description of
        //   Link-MaxLife above.  The specific variable names are used for
        //   demonstration purposes only, and an implementation is not required to
        //   use these names for these configuration variables.  For each
        //   configuration variable below, the default value is specified to
        //   simplify configuration.  In particular, the default values given
        //   below are chosen for a DSR network where nodes move at relative
        //   velocities between 12 and 25 seconds per wireless transmission
        //   radius.
        //
        //      InitStability                       25   seconds
        //      StabilityIncrFactor                  4
        //      StabilityDecrFactor                0.5
        //
        //      MinLifetime                          1   second
        //      UseExtends                         120   seconds


    /* =========================
       Configuration parameters
       ========================= */

        private double INIT_STABILITY = 25.0;          // seconds
        private double STABILITY_INCR_FACTOR = 4.0;
        private double STABILITY_DECR_FACTOR = 0.5;

        private double MIN_LIFETIME = 1.0;             // seconds
        private double USE_EXTENDS = 120.0;            // seconds


        // Undirected graph: node -> neighbor -> link
        private final Map<IP, Map<IP, Link>> graph = new HashMap<>();

        // Stability table: node -> stability value (seconds)
        private final Map<IP, Double> stabilityTable = new HashMap<>();

        @Override
        public void render(RenderUtil renderer) {
            var poseStack = renderer.getPoseStack();

            long now = sim.getSimTime();

            poseStack.pushPose();
            var offset = getNode().getPos();

            poseStack.scale(0.5f, 0.5f, 0.5f);
            renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "Routing Cache", renderer.getWidth() * 2, -10);
            poseStack.scale(2, 2, 2);


            poseStack.translate(0, 50, -1);
            var x1R = -50;
            var y1R = -50;
            var x2R = 50;
            var y2R = 50;
            renderer.drawLine(x1R, y1R, x1R, y2R, 0xFFFFFFFF);
            renderer.drawLine(x1R, y2R, x2R, y2R, 0xFFFFFFFF);
            renderer.drawLine(x2R, y2R, x2R, y1R, 0xFFFFFFFF);
            renderer.drawLine(x2R, y1R, x1R, y1R, 0xFFFFFFFF);

            var nodesToRender = new ArrayList<INetworkNode>();
            for (Link link : graph.values().stream().flatMap(it -> it.values().stream()).distinct().toList()) {


                float lifetimeRatio = computeLifetimeRatio(link, now);

                float r = 1.0f - lifetimeRatio;
                float g = lifetimeRatio;


                INetworkNode nodeA = sim.getNode(link.a);
                INetworkNode nodeB = sim.getNode(link.b);
                if (nodeA == null || nodeB == null) continue;
                nodesToRender.add(nodeA);
                nodesToRender.add(nodeB);

                Vec3 posA = nodeA.getPos().subtract(offset).scale(RENDER_SCALE);
                Vec3 posB = nodeB.getPos().subtract(offset).scale(RENDER_SCALE);


                float x1 = (float) posA.x;
                float y1 = (float) posA.z;
                float x2 = (float) posB.x;
                float y2 = (float) posB.z;

                var clipped = clipLine(x1, y1, x2, y2, x1R, y1R, x2R, y2R);

                if (clipped != null) {
                    renderer.drawLine(
                            clipped.x1, clipped.y1,
                            clipped.x2, clipped.y2,
                            0xFF000000 | ((int) (r * 255)) << 16 | ((int) (g * 255)) << 8
                    );
                }
            }

            for (var node : nodesToRender) {
                var pos = node.getPos().subtract(offset).scale(RENDER_SCALE);
                float x = (float) pos.x();
                float y = (float) pos.z();

                if (!isInside(x, y, x1R, y1R, x2R, y2R)) continue;

                poseStack.pushPose();
                poseStack.scale(0.25f, 0.25f, 0);
                renderer.drawString(node.getIP().toString(), 0x00ff00, (float) pos.x() * 4 - 15, (float) pos.z() * 4 - 12, false);
                poseStack.popPose();

                renderer.fillRectangle((float) (pos.x() - 1), (float) (pos.z() - 1), (float) (pos.x() + 1), (float) (pos.z() + 1), 0xFFFFFFFF);
            }

            poseStack.popPose();
        }

        private boolean isInside(float x, float y, float minX, float minY, float maxX, float maxY) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY;
        }

        private static class ClippedLine {
            float x1, y1, x2, y2;

            ClippedLine(float x1, float y1, float x2, float y2) {
                this.x1 = x1;
                this.y1 = y1;
                this.x2 = x2;
                this.y2 = y2;
            }
        }

        private ClippedLine clipLine(
                float x1, float y1,
                float x2, float y2,
                float minX, float minY,
                float maxX, float maxY
        ) {
            float dx = x2 - x1;
            float dy = y2 - y1;

            float t0 = 0.0f;
            float t1 = 1.0f;

            float[] p = {-dx, dx, -dy, dy};
            float[] q = {x1 - minX, maxX - x1, y1 - minY, maxY - y1};

            for (int i = 0; i < 4; i++) {
                if (p[i] == 0) {
                    if (q[i] < 0) return null; // parallel and outside
                } else {
                    float t = q[i] / p[i];

                    if (p[i] < 0) {
                        if (t > t1) return null;
                        if (t > t0) t0 = t;
                    } else {
                        if (t < t0) return null;
                        if (t < t1) t1 = t;
                    }
                }
            }

            return new ClippedLine(
                    x1 + t0 * dx,
                    y1 + t0 * dy,
                    x1 + t1 * dx,
                    y1 + t1 * dy
            );
        }

        @Override
        public Vec2 getRenderSize(Font font) {
            return new Vec2(100, 105);
        }

        private float computeLifetimeRatio(Link link, long now) {
            long remaining = link.remainingLifetime(now);
            float max = (float) (USE_EXTENDS * 1000.0);

            return Mth.clamp(remaining / max, 0.0f, 1.0f);
        }


        private class Link {
            IP a;
            IP b;

            long expirationTime;     // absolute time in ms
            long lastUsedTime;       // absolute time in ms

            Link(IP a, IP b, long expirationTime) {
                this.a = a;
                this.b = b;
                this.expirationTime = expirationTime;
                this.lastUsedTime = sim.getSimTime();
            }

            boolean isExpired(long now) {
                return expirationTime < now;
            }

            long remainingLifetime(long now) {
                return Math.max(0, expirationTime - now);
            }
        }

        public void addRoute(List<IP> route) {
            if (route == null || route.size() < 2) return;

            long now = sim.getSimTime();

            for (int i = 0; i < route.size() - 1; i++) {
                IP u = route.get(i);
                IP v = route.get(i + 1);
                addLink(u, v, now);
            }
        }

        public boolean hasRoute(IP destinationIP) {
            if (getNode().getIP().equals(destinationIP)) return true;
            return getRoute(destinationIP) != null;
        }

        public List<IP> getRoute(IP destinationIP) {
            long now = sim.getSimTime();
            purgeExpiredLinks(now);

            if (!graph.containsKey(getNode().getIP()) || !graph.containsKey(destinationIP)) return null;

            RouteCandidate candidate = computeBestRoute(getNode().getIP(), destinationIP, now);
            if (candidate == null) return null;

            // Extend lifetimes of used links
            extendLinksOnUse(candidate.path, now);

            return candidate.path;
        }

        private void addLink(IP u, IP v, long now) {
            initializeStability(u);
            initializeStability(v);

            double stabilityU = stabilityTable.get(u);
            double stabilityV = stabilityTable.get(v);

            double linkLifetimeSeconds = Math.max(MIN_LIFETIME, Math.min(stabilityU, stabilityV));

            long expirationTime = now + (long) (linkLifetimeSeconds * 1000);

            Link link = new Link(u, v, expirationTime);

            graph.computeIfAbsent(u, k -> new HashMap<>()).put(v, link);
            graph.computeIfAbsent(v, k -> new HashMap<>()).put(u, link);
        }

        private void purgeExpiredLinks(long now) {
            for (Map<IP, Link> neighbors : graph.values()) {
                neighbors.values().removeIf(link -> link.isExpired(now));
            }
        }

        private void removeLink(Link link) {
            if (link == null) return;

            graph.getOrDefault(link.a, Collections.emptyMap()).remove(link.b);
            graph.getOrDefault(link.b, Collections.emptyMap()).remove(link.a);

            // Stability decrease on break
            stabilityTable.computeIfPresent(link.a, (k, v) -> v * STABILITY_DECR_FACTOR);
            stabilityTable.computeIfPresent(link.b, (k, v) -> v * STABILITY_DECR_FACTOR);
        }


        public Link getLink(IP ip, IP nextAddress) {
            return graph.get(ip).get(nextAddress);
        }

        private void extendLinksOnUse(List<IP> path, long now) {
            for (int i = 0; i < path.size() - 1; i++) {
                IP u = path.get(i);
                IP v = path.get(i + 1);

                Link link = graph.get(u).get(v);
                if (link == null) continue;

                long timeSinceLastUse = now - link.lastUsedTime;
                double deltaSeconds = timeSinceLastUse / 1000.0;

                // Stability increase
                stabilityTable.computeIfPresent(u, (k, val) -> val + deltaSeconds * STABILITY_INCR_FACTOR);
                stabilityTable.computeIfPresent(v, (k, val) -> val + deltaSeconds * STABILITY_INCR_FACTOR);

                // Lifetime extension
                long minExpiration = now + (long) (USE_EXTENDS * 1000);
                if (link.expirationTime < minExpiration) {
                    link.expirationTime = minExpiration;
                }

                link.lastUsedTime = now;
            }
        }

        private void initializeStability(IP ip) {
            stabilityTable.putIfAbsent(ip, INIT_STABILITY);
        }

        private static class RouteCandidate {
            List<IP> path;
            int hopCount;
            long minLifetime;

            RouteCandidate(List<IP> path, int hopCount, long minLifetime) {
                this.path = path;
                this.hopCount = hopCount;
                this.minLifetime = minLifetime;
            }
        }

        private RouteCandidate computeBestRoute(IP src, IP dst, long now) {

            Map<IP, Integer> dist = new HashMap<>();
            Map<IP, Long> bestMinLifetime = new HashMap<>();
            Map<IP, IP> prev = new HashMap<>();

            PriorityQueue<IP> pq = new PriorityQueue<>(Comparator.comparingInt((IP ip) -> dist.get(ip)).thenComparing((IP n) -> -bestMinLifetime.get(n)));

            for (IP node : graph.keySet()) {
                dist.put(node, Integer.MAX_VALUE);
                bestMinLifetime.put(node, 0L);
            }

            dist.put(src, 0);
            bestMinLifetime.put(src, Long.MAX_VALUE);

            pq.add(src);

            while (!pq.isEmpty()) {
                IP current = pq.poll();

                if (current.equals(dst)) break;

                for (Map.Entry<IP, Link> entry : graph.getOrDefault(current, Collections.emptyMap()).entrySet()) {

                    IP neighbor = entry.getKey();
                    Link link = entry.getValue();

                    long remaining = link.remainingLifetime(now);
                    if (remaining <= 0) continue;

                    int newDist = dist.get(current) + 1;
                    long newMinLifetime = Math.min(bestMinLifetime.get(current), remaining);

                    boolean better = newDist < dist.get(neighbor) || (newDist == dist.get(neighbor) && newMinLifetime > bestMinLifetime.get(neighbor));

                    if (better) {
                        dist.put(neighbor, newDist);
                        bestMinLifetime.put(neighbor, newMinLifetime);
                        prev.put(neighbor, current);
                        pq.remove(neighbor);
                        pq.add(neighbor);
                    }
                }
            }

            if (!prev.containsKey(dst) && !src.equals(dst)) return null;

            List<IP> path = new LinkedList<>();
            IP step = dst;
            path.addFirst(step);

            while (prev.containsKey(step)) {
                step = prev.get(step);
                path.addFirst(step);
            }

            return new RouteCandidate(path, dist.get(dst), bestMinLifetime.get(dst));
        }


        @Override
        public String toString() {
            return new StringJoiner(", ", RouteCache.class.getSimpleName() + "[", "]")
                    .add("graph=" + graph)
                    .add("stabilityTable=" + stabilityTable)
                    .toString();
        }
    }


    @Override
    public ProtocolSettings getSettings() {
        return settings;
    }
}
