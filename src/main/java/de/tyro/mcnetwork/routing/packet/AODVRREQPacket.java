package de.tyro.mcnetwork.routing.packet;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.MathUtil;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec2;

import java.util.UUID;

public class AODVRREQPacket extends NetworkPacket implements IProtocolPaket {
    public final boolean joinFlag;
    public final boolean repairFlag;
    public final boolean gratuitousFlag;
    public final boolean destinationOnlyFlag;
    public final boolean unknownSeqFlag;

    public int hopCount;
    public final int rreqId;
    public int destinationSequenceNumber;
    public int originatorSequenceNumber;

    public AODVRREQPacket(IP sourceIp, IP destinationIp, boolean joinFlag, boolean repairFlag, boolean gratuitousFlag, boolean destinationOnlyFlag, boolean unknownSeqFlag, int hopCount, int rreqId, int destinationSequenceNumber, int originatorSequenceNumber) {
        super(sourceIp, destinationIp);
        this.joinFlag = joinFlag;
        this.repairFlag = repairFlag;
        this.gratuitousFlag = gratuitousFlag;
        this.destinationOnlyFlag = destinationOnlyFlag;
        this.unknownSeqFlag = unknownSeqFlag;
        this.hopCount = hopCount;
        this.rreqId = rreqId;
        this.destinationSequenceNumber = destinationSequenceNumber;
        this.originatorSequenceNumber = originatorSequenceNumber;
    }

    public AODVRREQPacket(IP sourceIp, IP destinationIp, boolean unknownSeqFlag, int destinationSequenceNumber, int originatorSequenceNumber, int rreqId, int hopCount) {
        this(sourceIp, destinationIp, false, false, false, false, unknownSeqFlag, hopCount, rreqId, destinationSequenceNumber, originatorSequenceNumber);
    }

    public AODVRREQPacket(UUID uuid, IP originatorIP, IP destinationIP, boolean joinFlag, boolean repairFlag, boolean gratuitousFlag, boolean destinationOnlyFlag, boolean unknownSeqFlag, int hopCount, int rreqId, int destinationSequenceNumber, int originatorSequenceNumber) {
        super(uuid, originatorIP, destinationIP);
        this.joinFlag = joinFlag;
        this.repairFlag = repairFlag;
        this.gratuitousFlag = gratuitousFlag;
        this.destinationOnlyFlag = destinationOnlyFlag;
        this.unknownSeqFlag = unknownSeqFlag;
        this.hopCount = hopCount;
        this.rreqId = rreqId;
        this.destinationSequenceNumber = destinationSequenceNumber;
        this.originatorSequenceNumber = originatorSequenceNumber;
    }

    public NetworkPacket hop(INetworkNode self) {
        return new AODVRREQPacket(originatorIP, destinationIP, joinFlag, repairFlag, gratuitousFlag, destinationOnlyFlag, unknownSeqFlag, hopCount++, rreqId, destinationSequenceNumber, originatorSequenceNumber);
    }

    @Override
    protected void renderPacketContent(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float alpha, Font font, float width) {

        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        RenderUtil.drawString(RenderUtil.Align.LEFT, "Flags", RenderUtil.Color.MAGENTA.value, width * 2, 3, poseStack, buffer, packedLight);

        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.RIGHT,"J|R|G|D|U", alpha, width * 2, 0, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.RIGHT,(joinFlag ? 1 : 0) + "|" + (repairFlag ? 1 : 0) + "|" + (gratuitousFlag ? 1 : 0) + "|" + (destinationOnlyFlag ? 1 : 0) + "|" + (unknownSeqFlag ? 1 : 0), alpha, width * 2, 8, poseStack, buffer, packedLight);

        poseStack.translate(0, 17, 0);
        RenderUtil.renderHLine(alpha, width * 2, 0, poseStack, buffer, packedLight);
        RenderUtil.drawString(RenderUtil.Align.LEFT, "Hops", RenderUtil.Color.MAGENTA.value, width * 2, 2, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.RIGHT,String.valueOf(hopCount), alpha, width * 2, 2, poseStack, buffer, packedLight);

        poseStack.translate(0, 11, 0);
        RenderUtil.renderHLine(alpha, width * 2, 0, poseStack, buffer, packedLight);
        RenderUtil.drawString(RenderUtil.Align.LEFT, "SQ Num", RenderUtil.Color.MAGENTA.value, width * 2, 3, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, getOriginatorIP() + ": " + originatorSequenceNumber, alpha, width * 2, 2, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, getDestinationIP() + ": " + (unknownSeqFlag ? "?" : destinationSequenceNumber), alpha, width * 2, 10, poseStack, buffer, packedLight);

        poseStack.translate(0, 18, 0);
        RenderUtil.renderHLine(alpha, width * 2, 0, poseStack, buffer, packedLight);
        RenderUtil.drawString(RenderUtil.Align.LEFT, "RREQ ID", RenderUtil.Color.MAGENTA.value, width * 2, 2, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, String.valueOf(rreqId), alpha, width * 2, 2, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    @Override
    public Vec2 getRenderSize(Font font) {
        var width = MathUtil.max(font.width("SQ @ " + getDestinationIP() + ": " + (unknownSeqFlag ? "?" : destinationSequenceNumber)), font.width("SQ @ " + getOriginatorIP() + ": " + originatorSequenceNumber)) / 2;
        return new Vec2(width, 28);
    }

    @Override
    public INetworkPacket copy() {
        return new AODVRREQPacket(originatorIP, destinationIP, joinFlag, repairFlag, gratuitousFlag, destinationOnlyFlag, unknownSeqFlag, hopCount, rreqId, destinationSequenceNumber, originatorSequenceNumber);
    }
}
