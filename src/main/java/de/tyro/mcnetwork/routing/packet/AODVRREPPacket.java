package de.tyro.mcnetwork.routing.packet;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.IP;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec2;

import java.util.UUID;

public class AODVRREPPacket extends NetworkPacket implements IProtocolPaket {

    public boolean repairFlag;
    public boolean ackRequiredFlag;
    public int hopCount;
    public int destSeqNumber;
    public long lifetime;


    public AODVRREPPacket(IP originatorIpAddress, IP destinationIpAddress, boolean repairFlag, boolean ackRequiredFlag, int hopCount, int destSeqNumber, long lifetime) {
        super(originatorIpAddress, destinationIpAddress);
        this.repairFlag = repairFlag;
        this.ackRequiredFlag = ackRequiredFlag;
        this.hopCount = hopCount;
        this.destSeqNumber = destSeqNumber;
        this.lifetime = lifetime;
    }

    public AODVRREPPacket(UUID uuid, IP originatorIP, IP destinationIP, boolean repairFlag, boolean ackRequiredFlag, int hopCount, int destSeqNumber, long lifetime) {
        super(uuid, originatorIP, destinationIP);
        this.repairFlag = repairFlag;
        this.ackRequiredFlag = ackRequiredFlag;
        this.hopCount = hopCount;
        this.destSeqNumber = destSeqNumber;
        this.lifetime = lifetime;
    }

    @Override
    protected void renderPacketContent(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float alpha, Font font, float width) {
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        width *= 2;
        RenderUtil.drawString(RenderUtil.Align.LEFT, "Flags", RenderUtil.Color.MAGENTA.value, width, 3, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, "R|A", alpha, width, 0, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, (repairFlag ? 1 : 0) +  "|" + (ackRequiredFlag ? 1 : 0), alpha, width, 8, poseStack, buffer, packedLight);

        poseStack.translate(0, 17, 0);
        RenderUtil.renderHLine(alpha, width, 0, poseStack, buffer, packedLight);
        RenderUtil.drawString(RenderUtil.Align.LEFT, "Hops", RenderUtil.Color.MAGENTA.value, width, 2, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.RIGHT,String.valueOf(hopCount), alpha, width, 2, poseStack, buffer, packedLight);

        poseStack.translate(0, 11, 0);
        RenderUtil.renderHLine(alpha, width, 0, poseStack, buffer, packedLight);
        RenderUtil.drawString(RenderUtil.Align.LEFT, "SQ Num", RenderUtil.Color.MAGENTA.value, width, 2, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, getOriginatorIP() + ": " + destSeqNumber, alpha, width, 2, poseStack, buffer, packedLight);

        poseStack.translate(0, 11, 0);
        RenderUtil.renderHLine(alpha, width, 0, poseStack, buffer, packedLight);
        RenderUtil.drawString(RenderUtil.Align.LEFT, "Lifetime", RenderUtil.Color.MAGENTA.value, width, 2, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, String.valueOf(lifetime), alpha, width, 2, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    @Override
    protected Vec2 getContentSize(Font font) {
        var width = font.width("Lifetime " + lifetime)/2;

        return new Vec2(width, 24);
    }

    @Override
    public INetworkPacket copy() {
        return new AODVRREPPacket(originatorIP, destinationIP, repairFlag, ackRequiredFlag, hopCount, destSeqNumber, lifetime);
    }
}
