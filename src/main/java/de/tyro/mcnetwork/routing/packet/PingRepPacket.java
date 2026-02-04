package de.tyro.mcnetwork.routing.packet;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.MathUtil;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.SimulationEngine;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec2;

import java.util.StringJoiner;
import java.util.UUID;

public class PingRepPacket extends NetworkPacket implements IApplicationPaket {
    public final long sendTime;
    public final long returnStartTime;
    public final UUID replyUUID;

    public PingRepPacket(IP sourceIp, IP destinationIp, long sendTime, long returnStartTime, UUID replyUUID) {
        super(sourceIp, destinationIp);
        this.sendTime = sendTime;
        this.returnStartTime = returnStartTime;
        this.replyUUID = replyUUID;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PingRepPacket.class.getSimpleName() + "[", "]").add("sendTime=" + sendTime).add("returnTime=" + returnStartTime).add("id=" + id).add("sourceIp=" + sourceIp).add("destinationIp=" + destinationIp).toString();
    }

    @Override
    protected void renderPacketContent(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float alpha, Font font, float width) {
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "Rep: " + replyUUID.toString().substring(0, 8), alpha, width, 0, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "T1: " + sendTime + "ms", alpha, width, 8, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "T2: " + (SimulationEngine.getInstance().getSimTime() - returnStartTime) + "ms", alpha, width, 16, poseStack, buffer, packedLight);
    }

    @Override
    protected Vec2 getContentSize(Font font) {
        var width = MathUtil.max(
                font.width("Rep: " + replyUUID.toString().substring(0, 8)),
                font.width("T1: " + sendTime + "ms"),
                font.width("T2: " + (SimulationEngine.getInstance().getSimTime() - returnStartTime) + "ms"));

        return new Vec2(width, font.lineHeight * 3);
    }

    @Override
    public INetworkPacket copy() {
        return new PingRepPacket(sourceIp, destinationIp, sendTime, returnStartTime, replyUUID);
    }
}
