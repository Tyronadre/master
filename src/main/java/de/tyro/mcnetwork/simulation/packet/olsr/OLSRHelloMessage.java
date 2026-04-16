package de.tyro.mcnetwork.simulation.packet.olsr;

import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.network.BetterByteBuf;
import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.protocol.OLSRProtocol;
import net.minecraft.client.gui.Font;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 6.1.  HELLO Message Format
 * <br><br>
 * To accommodate for link sensing, neighborhood detection and MPR
 * selection signalling, as well as to accommodate for future
 * extensions, an approach similar to the overall packet format is
 * taken.  Thus the proposed format of a HELLO message is as follows:
 * <br><br>
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1<br>
 * <br><br>
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+<br>
 * |          Reserved             |     Htime     |  Willingness  |<br>
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+<br>
 * |   Link Code   |   Reserved    |       Link Message Size       |<br>
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+<br>
 * |                  Neighbor Interface Address                   |<br>
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+<br>
 * |                  Neighbor Interface Address                   |<br>
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+<br>
 * :                             .  .  .                           :<br>
 * :                                                               :<br>
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+<br>
 * |   Link Code   |   Reserved    |       Link Message Size       |<br>
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+<br>
 * |                  Neighbor Interface Address                   |<br>
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+<br>
 * |                  Neighbor Interface Address                   |<br>
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+<br>
 * :                                                               :<br>
 * :                                       :
 * (etc.)
 * <br><br>
 * This is sent as the data-portion of the general packet format
 * described in section 3.4, with the "Message Type" set to
 * HELLO_MESSAGE, the TTL field set to 1 (one) and Vtime set accordingly
 * to the value of NEIGHB_HOLD_TIME, specified in section 18.3.
 * <br><br>
 * Reserved
 * <br><br>
 * This field must be set to "0000000000000" to be in compliance
 * with this specification.
 * <br><br>
 * HTime
 * <br><br>
 * This field specifies the HELLO emission interval used by the
 * node on this particular interface, i.e., the time before the
 * transmission of the next HELLO (this information may be used in
 * advanced link sensing, see section 14).  The HELLO emission
 * interval is represented by its mantissa (four highest bits of
 * Htime field) and by its exponent (four lowest bits of Htime
 * field).  In other words:
 * <br><br>
 * HELLO emission interval=C*(1+a/16)*2^b  [in seconds]
 * <br><br>
 * where a is the integer represented by the four highest bits of
 * Htime field and b the integer represented by the four lowest
 * bits of Htime field.  The proposed value of the scaling factor
 * C is specified in section 18.
 * <br><br>
 * Willingness
 * <br><br>
 * This field specifies the willingness of a node to carry and
 * forward traffic for other nodes.
 * <br><br>
 * A node with willingness WILL_NEVER (see section 18.8, for
 * willingness constants) MUST never be selected as MPR by any
 * node.  A node with willingness WILL_ALWAYS MUST always be
 * selected as MPR.  By default, a node SHOULD advertise a
 * willingness of WILL_DEFAULT.
 * <br><br>
 * Link Code
 * <br><br>
 * This field specifies information about the link between the
 * interface of the sender and the following list of neighbor
 * interfaces.  It also specifies information about the status of
 * the neighbor.
 * <br><br>
 * Link codes, not known by a node, are silently discarded.
 * <br><br>
 * Link Message Size
 * <br><br>
 * The size of the link message, counted in bytes and measured
 * from the beginning of the "Link Code" field and until the next
 * "Link Code" field (or - if there are no more link types - the
 * end of the message).
 * <br><br>
 * Neighbor Interface Address
 * <br><br>
 * The address of an interface of a neighbor node.
 */
public record OLSRHelloMessage(long hTime, List<OLSRHelloMessageEntry> entries) implements OLSRMessage {

    public static final StreamCodec<BetterByteBuf, OLSRHelloMessage> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(BetterByteBuf buffer, OLSRHelloMessage value) {
            buffer.writeLong(value.hTime);
            buffer.writeInt(value.entries.size());
            for (OLSRHelloMessageEntry entry : value.entries) {
                OLSRHelloMessageEntry.STREAM_CODEC.encode(buffer, entry);
            }
        }

        @Override
        public OLSRHelloMessage decode(BetterByteBuf buffer) {
            long hTime = buffer.readLong();
            int size = buffer.readInt();
            List<OLSRHelloMessageEntry> entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                entries.add(OLSRHelloMessageEntry.STREAM_CODEC.decode(buffer));
            }
            return new OLSRHelloMessage(hTime, entries);
        }
    };

    @Override
    public void render(RenderUtil renderer) {
        var pose = renderer.getPoseStack();
        var font = renderer.getFont();
        var width = renderer.getWidth();
        var y = 0;
        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "HTime: ", width, y);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, String.valueOf(hTime), width, y);
        y += 20;
        pose.scale(0.5f, 0.5f, 0.5f);
        var x = -width;
        renderer.drawString("Will", RenderUtil.Color.WHITE.value, x, y, false);
        x += font.width("Will") + 30;
        renderer.drawString("Neigh.", RenderUtil.Color.WHITE.value, x, y, false);
        x += font.width("Neigh.") + 10;
        renderer.drawString("Link", RenderUtil.Color.WHITE.value, x, y, false);
        renderer.drawString("Addresses", RenderUtil.Color.WHITE.value, width - font.width("Addresses"), y, false);
        y += 10;

        for (var entry : entries) {
            x = -width;
            renderer.drawString(entry.willingness.toString(), RenderUtil.Color.WHITE.value, x, y, false);
            x += font.width("Will") + 30;
            renderer.drawString(entry.neighborType.toString(), RenderUtil.Color.WHITE.value, x, y, false);
            x += font.width("Neigh.") + 10;
            renderer.drawString(entry.linkType.toString(), RenderUtil.Color.WHITE.value, x, y, false);

            for (Iterator<IP> iterator = entry.neighborAddresses.iterator(); iterator.hasNext(); ) {
                var neighborAddress = iterator.next().toString();

                renderer.drawString(neighborAddress, RenderUtil.Color.WHITE.value, width - font.width(neighborAddress), y, false);
                if (iterator.hasNext()) {
                    var neighborAddress2 = iterator.next().toString();
                    renderer.drawString(neighborAddress2, RenderUtil.Color.WHITE.value, width - font.width(neighborAddress2) - font.width(IP.BROADCAST.toString()), y, false);
                    if (iterator.hasNext()) y += 10;
                }
            }

            y += 15;
        }
    }

    @Override
    public Vec2 getRenderSize(Font font) {
        var height = 5f; //htim
        height += 4f; //header
        for (var e : entries) {
            height += (float) (Math.ceil(e.neighborAddresses.size() / 2f) * 2.5f);
            height += 0.5f;
        }

        return new Vec2(140, height);
    }

    public static class OLSRHelloMessageEntry {
        private final OLSRProtocol.Willingness willingness;
        private final OLSRProtocol.NeighborType neighborType;
        private final OLSRProtocol.LinkType linkType;
        private final Set<IP> neighborAddresses;

        public OLSRHelloMessageEntry(OLSRProtocol.Willingness willingness, OLSRProtocol.NeighborType neighborType, OLSRProtocol.LinkType linkType) {
            this.willingness = willingness;
            this.neighborType = neighborType;
            this.linkType = linkType;
            this.neighborAddresses = new HashSet<>();
        }

        private OLSRHelloMessageEntry(OLSRProtocol.Willingness willingness, OLSRProtocol.NeighborType neighborType, OLSRProtocol.LinkType linkType, HashSet<IP> ips) {
            this.willingness = willingness;
            this.neighborType = neighborType;
            this.linkType = linkType;
            this.neighborAddresses = ips;
        }

        public void addNeighbor(IP neighbor) {
            this.neighborAddresses.add(neighbor);
        }

        public OLSRProtocol.LinkType getLinkType() {
            return linkType;
        }

        public OLSRProtocol.Willingness getWillingness() {
            return willingness;
        }

        public OLSRProtocol.NeighborType getNeighborType() {
            return neighborType;
        }

        public Set<IP> getNeighborAddresses() {
            return neighborAddresses;
        }


        @Override
        public String toString() {
            return willingness.toString() + "  " + neighborType.name() + " " + linkType.name() + " (" + neighborAddresses.stream().map(IP::toString).reduce((a, b) -> a + " " + b).orElse("") + ")";
        }

        public static final StreamCodec<BetterByteBuf, OLSRHelloMessageEntry> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public @NotNull OLSRHelloMessageEntry decode(BetterByteBuf buffer) {
                return new OLSRHelloMessageEntry(
                        buffer.readEnum(OLSRProtocol.Willingness.class),
                        buffer.readEnum(OLSRProtocol.NeighborType.class),
                        buffer.readEnum(OLSRProtocol.LinkType.class),
                        buffer.readIPCollection(HashSet::new)
                );
            }

            @Override
            public void encode(BetterByteBuf buffer, OLSRHelloMessageEntry value) {
                buffer.writeEnum(value.willingness);
                buffer.writeEnum(value.neighborType);
                buffer.writeEnum(value.linkType);
                buffer.writeIPCollection(value.neighborAddresses);
            }
        };
    }
}
