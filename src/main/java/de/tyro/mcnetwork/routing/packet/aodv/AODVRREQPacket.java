package de.tyro.mcnetwork.routing.packet.aodv;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.util.MathUtil;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;
import net.minecraft.client.gui.Font;
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
        return new AODVRREQPacket(getOriginatorIP(), getDestinationIP(), joinFlag, repairFlag, gratuitousFlag, destinationOnlyFlag, unknownSeqFlag, hopCount++, rreqId, destinationSequenceNumber, originatorSequenceNumber);
    }

    @Override
    protected void renderPacketContent(RenderUtil renderer, PoseStack poseStack, float width) {

        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        renderer.drawString(RenderUtil.Align.LEFT, "Flags", RenderUtil.Color.MAGENTA.value, width * 2, 3);

        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, "J|R|G|D|U", width * 2, 0);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, (joinFlag ? 1 : 0) + "|" + (repairFlag ? 1 : 0) + "|" + (gratuitousFlag ? 1 : 0) + "|" + (destinationOnlyFlag ? 1 : 0) + "|" + (unknownSeqFlag ? 1 : 0), width * 2, 8);

        poseStack.translate(0, 17, 0);
        renderer.renderHLineWithAlphaColor(width * 2, 0);
        renderer.drawString(RenderUtil.Align.LEFT, "Hops", RenderUtil.Color.MAGENTA.value, width * 2, 2);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, String.valueOf(hopCount), width * 2, 2);

        poseStack.translate(0, 11, 0);
        renderer.renderHLineWithAlphaColor(width * 2, 0);
        renderer.drawString(RenderUtil.Align.LEFT, "SQ Num", RenderUtil.Color.MAGENTA.value, width * 2, 3);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, getOriginatorIP() + ": " + originatorSequenceNumber, width * 2, 2);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, getDestinationIP() + ": " + (unknownSeqFlag ? "?" : destinationSequenceNumber), width * 2, 10);

        poseStack.translate(0, 18, 0);
        renderer.renderHLineWithAlphaColor(width * 2, 0);
        renderer.drawString(RenderUtil.Align.LEFT, "RREQ ID", RenderUtil.Color.MAGENTA.value, width * 2, 2);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, String.valueOf(rreqId), width * 2, 2);

        poseStack.popPose();
    }

    @Override
    public Vec2 getRenderSize(Font font) {
        var width = MathUtil.max(font.width("SQ @ " + getDestinationIP() + ": " + (unknownSeqFlag ? "?" : destinationSequenceNumber)), font.width("SQ @ " + getOriginatorIP() + ": " + originatorSequenceNumber)) / 2;
        return new Vec2(width, 28);
    }

    @Override
    public INetworkPacket copy() {
        return new AODVRREQPacket(getId(), getOriginatorIP(), getDestinationIP(), joinFlag, repairFlag, gratuitousFlag, destinationOnlyFlag, unknownSeqFlag, hopCount, rreqId, destinationSequenceNumber, originatorSequenceNumber);
    }
}
