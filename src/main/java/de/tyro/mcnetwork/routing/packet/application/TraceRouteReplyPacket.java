package de.tyro.mcnetwork.routing.packet.application;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.util.MathUtil;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.IApplicationPacket;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec2;

import java.util.StringJoiner;
import java.util.UUID;

public class TraceRouteReplyPacket extends NetworkPacket implements IApplicationPacket {
    public final int sendTime;
    public long returnStartTime;
    public final UUID replyUUID;
    public final int hopCount;

    public TraceRouteReplyPacket(IP sourceIp, IP destinationIp, int sendTime, long returnStartTime, UUID replyUUID, int hopCount) {
        super(sourceIp, destinationIp);
        this.sendTime = sendTime;
        this.returnStartTime = returnStartTime;
        this.replyUUID = replyUUID;
        this.hopCount = hopCount;
    }

    public TraceRouteReplyPacket(UUID uuid, IP originatorIP, IP destinationIP, int sendTime, long returnStartTime, UUID uuid1, int hopCount) {
        super(uuid, originatorIP, destinationIP);
        this.sendTime = sendTime;
        this.returnStartTime = returnStartTime;
        this.replyUUID = uuid1;
        this.hopCount = hopCount;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TraceRouteReplyPacket.class.getSimpleName() + "[", "]")
                .add("sendTime=" + sendTime)
                .add("returnTime=" + returnStartTime)
                .add("hopCount=" + hopCount)
                .add("id=" + id)
                .add("sourceIp=" + getOriginatorIP())
                .add("destinationIp=" + getDestinationIP()).toString();
    }

    @Override
    protected void renderPacketContent(RenderUtil renderer, PoseStack poseStack, float width) {
        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "Rep: " + replyUUID.toString().substring(0, 8), width, 0);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "T1: " + sendTime + "ms", width, 8);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "T2: " + (getSimulationEngine().getSimTime() - returnStartTime) + "ms", width, 16);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "Hop: " + hopCount, width, 24);
    }

    @Override
    public Vec2 getRenderSize(Font font) {
        var width = MathUtil.max(
                font.width("Rep: " + replyUUID.toString().substring(0, 8)),
                font.width("T1: " + sendTime + "ms"),
                font.width("T2: " + (getSimulationEngine().getSimTime() - returnStartTime) + "ms"),
                font.width("Hop: " + hopCount));

        return new Vec2(width, font.lineHeight * 4);
    }

    @Override
    public INetworkPacket copy() {
        return new TraceRouteReplyPacket(id, getOriginatorIP(), getDestinationIP(), sendTime, returnStartTime, replyUUID, hopCount);
    }

}

