package de.tyro.mcnetwork.simulation.packet.olsr;

import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.network.BetterByteBuf;
import de.tyro.mcnetwork.simulation.IP;
import net.minecraft.client.gui.Font;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * 9.1.  TC Message Format
 *<br><br>
 *    The proposed format of a TC message is as follows:
 *<br><br>
 *        0                   1                   2                   3
 *        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |              ANSN             |           Reserved            |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |               Advertised Neighbor Main Address                |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |               Advertised Neighbor Main Address                |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |                              ...                              |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *<br><br>
 *    This is sent as the data-portion of the general message format with
 *    the "Message Type" set to TC_MESSAGE.  The time to live SHOULD be set
 *    to 255 (maximum value) to diffuse the message into the entire network
 *    and Vtime set accordingly to the value of TOP_HOLD_TIME, as specified
 *    in section 18.3.
 *<br><br>
 *      Advertised Neighbor Sequence Number (ANSN)
 *<br><br>
 *           A sequence number is associated with the advertised neighbor
 *           set.  Every time a node detects a change in its advertised
 *           neighbor set, it increments this sequence number ("Wraparound"
 *           is handled as described in section 19).  This number is sent
 *           in this ANSN field of the TC message to keep track of the most
 *           recent information.  When a node receives a TC message, it can
 *           decide on the basis of this Advertised Neighbor Sequence
 *           Number, whether or not the received information about the
 *           advertised neighbors of the originator node is more recent
 *           than what it already has.
 *<br><br>
 *      Advertised Neighbor Main Address
 *<br><br>
 *           This field contains the main address of a neighbor node.  All
 *           main addresses of the advertised neighbors of the Originator
 *           node are put in the TC message.  If the maximum allowed
 *           message size (as imposed by the network) is reached while
 *           there are still advertised neighbor addresses which have not
 *           been inserted into the TC-message, more TC messages will be
 *           generated until the entire advertised neighbor set has been
 *           sent.  Extra main addresses of neighbor nodes may be included,
 *           if redundancy is desired.
 *<br><br>
 *      Reserved
 *<br><br>
 *           This field is reserved, and MUST be set to "0000000000000000"
 *           for compliance with this document.
 */
public record OLSRTCMessage(int advertisedNeighborSequenceNumber, Set<IP> advertisedNeighborMainAddresses) implements OLSRMessage {

    public static final StreamCodec<BetterByteBuf, OLSRTCMessage> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull OLSRTCMessage decode(BetterByteBuf buffer) {
            return new OLSRTCMessage(
                    buffer.readInt(),
                    buffer.readIPCollection(HashSet::new)
            );
        }

        @Override
        public void encode(BetterByteBuf buffer, OLSRTCMessage value) {
            buffer.writeInt(value.advertisedNeighborSequenceNumber);
            buffer.writeIPCollection(value.advertisedNeighborMainAddresses);
        }
    };

    @Override
    public void render(RenderUtil renderer) {
        var pose = renderer.getPoseStack();
        var width = renderer.getWidth();

        var y = 0;
        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "ANSN: ", width, y);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, String.valueOf(advertisedNeighborSequenceNumber), width, y);
        y += 20;
        pose.scale(0.5f, 0.5f, 0.5f);
        for(var neighbor: advertisedNeighborMainAddresses) {
            renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, neighbor.toString(), width, y);
            y+=10;
        }
    }

    @Override
    public Vec2 getRenderSize(Font font) {
        return new Vec2(140,5 + advertisedNeighborMainAddresses.size() * 3f );
    }
}
