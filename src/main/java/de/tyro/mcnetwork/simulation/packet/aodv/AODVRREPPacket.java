package de.tyro.mcnetwork.simulation.packet.aodv;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.packet.INetworkPacket;
import de.tyro.mcnetwork.simulation.packet.IProtocolPaket;
import de.tyro.mcnetwork.simulation.packet.NetworkPacket;
import net.minecraft.client.gui.Font;
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
    protected void renderPacketContent(RenderUtil renderer, PoseStack poseStack, float width) {
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        width *= 2;
        renderer.drawString(RenderUtil.Align.LEFT, "Flags", RenderUtil.Color.MAGENTA.value, width, 3);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, "R|A",  width, 0);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, (repairFlag ? 1 : 0) +  "|" + (ackRequiredFlag ? 1 : 0), width, 8);

        poseStack.translate(0, 17, 0);
        renderer.renderHLineWithAlphaColor( width, 0);
        renderer.drawString(RenderUtil.Align.LEFT, "Hops", RenderUtil.Color.MAGENTA.value, width, 2);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT,String.valueOf(hopCount), width, 2);

        poseStack.translate(0, 11, 0);
        renderer.renderHLineWithAlphaColor( width, 0 );
        renderer.drawString(RenderUtil.Align.LEFT, "SQ Num", RenderUtil.Color.MAGENTA.value, width, 2);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, getOriginatorIP() + ": " + destSeqNumber, width, 2);

        poseStack.translate(0, 11, 0);
        renderer.renderHLineWithAlphaColor( width, 0  );
        renderer.drawString(RenderUtil.Align.LEFT, "Lifetime", RenderUtil.Color.MAGENTA.value, width, 2);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, String.valueOf(lifetime), width, 2);

        poseStack.popPose();
    }

    @Override
    public Vec2 getRenderSize(Font font) {
        var superSize = super.getRenderSize(font);
        var width = font.width("Lifetime " + lifetime)/2;

        return new Vec2(Math.max(superSize.x, width), superSize.y + 24);
    }

    @Override
    public INetworkPacket copy() {
        return new AODVRREPPacket(getId(), getOriginatorIP(), getDestinationIP(), repairFlag, ackRequiredFlag, hopCount, destSeqNumber, lifetime);
    }
}
