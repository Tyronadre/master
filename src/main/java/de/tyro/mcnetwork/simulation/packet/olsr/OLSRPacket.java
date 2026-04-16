package de.tyro.mcnetwork.simulation.packet.olsr;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.network.BetterByteBuf;
import de.tyro.mcnetwork.network.payload.networkPacket.NetworkPacketCodec;
import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.packet.INetworkPacket;
import de.tyro.mcnetwork.simulation.packet.IProtocolPaket;
import de.tyro.mcnetwork.simulation.packet.NetworkPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec2;

import java.util.UUID;

public class OLSRPacket extends NetworkPacket implements IProtocolPaket {
    private final int vTime;
    private final int messageSequenceNumber;
    private final Type type;
    private final OLSRMessage messageBlock;

    public enum Type {
        HELLO,
        TC
    }

    public OLSRPacket(IP originatorIP, Type type, int vTime, int messageSequenceNumber, OLSRMessage messageBlock) {
        super(originatorIP, IP.BROADCAST);
        this.type = type;
        this.vTime = vTime;
        this.messageSequenceNumber = messageSequenceNumber;
        this.messageBlock = messageBlock;
    }

    protected OLSRPacket(UUID uuid, IP originatorIP, Type type, int vTime, int messageSequenceNumber, OLSRMessage messageBlock) {
        super(uuid, originatorIP, IP.BROADCAST);
        this.type = type;
        this.vTime = vTime;
        this.messageSequenceNumber = messageSequenceNumber;
        this.messageBlock = messageBlock;
    }

    @Override
    public INetworkPacket copy() {
        return new OLSRPacket(getId(), getOriginatorIP(), type, vTime, messageSequenceNumber, messageBlock);
    }

    public Integer getSequenceNumber() {
        return messageSequenceNumber;
    }

    public int getVTime() {
        return vTime;
    }

    public OLSRMessage getMessageBlock() {
        return messageBlock;
    }

    public static final NetworkPacketCodec<OLSRPacket> STREAM_CODEC = new NetworkPacketCodec<>() {
        @Override
        protected OLSRPacket decodeActual(UUID uuid, IP originatorIP, IP destinationIP, BetterByteBuf buf) {
            var type = buf.readEnum(Type.class);
            var vTime = buf.readInt();
            var messageSequenceNumber = buf.readInt();
            var message = switch (type) {
                case TC -> OLSRTCMessage.STREAM_CODEC.decode(buf);
                case HELLO -> OLSRHelloMessage.STREAM_CODEC.decode(buf);
            };
            return new OLSRPacket(uuid, originatorIP, type, vTime, messageSequenceNumber, message);
        }

        @Override
        protected void encodeActual(BetterByteBuf buffer, OLSRPacket packet) {
            buffer.writeEnum(packet.type);
            buffer.writeInt(packet.vTime);
            buffer.writeInt(packet.messageSequenceNumber);
            switch (packet.type) {
                case TC -> OLSRTCMessage.STREAM_CODEC.encode(buffer, (OLSRTCMessage) packet.messageBlock);
                case HELLO -> OLSRHelloMessage.STREAM_CODEC.encode(buffer, (OLSRHelloMessage) packet.messageBlock);
            }
        }
    };

    @Override
    protected void renderPacketContent(RenderUtil renderer, PoseStack poseStack, float width) {
        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);
        width *= 2;
        var y = 0;
        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "Type: ", width, y);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, type.name(), width, y);
        y += 10;
        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "VTime: ", width, y);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, String.valueOf(vTime), width, y);
        y += 10;
        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "SeqNum: ", width, y);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, String.valueOf(messageSequenceNumber), width, y);
        y += 10;
        renderer.renderHLineWithAlphaColor(width, y);
        y += 5;
        poseStack.translate(0,y,0);
        renderer.setWidth(width);
        messageBlock.render(renderer);
        poseStack.popPose();
    }

    @Override
    public Vec2 getRenderSize(Font font) {
        var superSize = super.getRenderSize(font);

        var messageBlockSize = messageBlock.getRenderSize(font);
        return new Vec2(superSize.x, 15 + messageBlockSize.y + superSize.y);
    }
}
