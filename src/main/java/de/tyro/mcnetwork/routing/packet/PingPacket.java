package de.tyro.mcnetwork.routing.packet;


import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.SimulationEngine;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;

import java.util.StringJoiner;
import java.util.UUID;

public class PingPacket extends NetworkPacket implements IApplicationPaket {

    public final long sendStartTime;

    public PingPacket(IP src, IP dst, long sendStartTime) {
        super(src, dst);
        this.sendStartTime = sendStartTime;
    }

    public PingPacket(UUID uuid,  IP src, IP dst, long sendStartTime) {
        super(uuid, src, dst);
        this.sendStartTime = sendStartTime;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PingPacket.class.getSimpleName() + "[", "]")
                .add("sendTime=" + sendStartTime)
                .add("id=" + id)
                .add("sourceIp=" + originatorIP)
                .add("destinationIp=" + destinationIP)
                .toString();
    }

    @Override
    protected Vec2 getContentSize(Font font) {
        var line = "Time " + (SimulationEngine.getInstance().getSimTime() - sendStartTime) + "ms";
        return new Vec2(font.width(line), font.lineHeight);
    }

    @Override
    protected void renderPacketContent(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float alpha, Font font, float width) {
        String line = "Time " + (SimulationEngine.getInstance().getSimTime() - sendStartTime) + "ms";
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.LEFT, line, alpha, width, 0, poseStack, buffer, packedLight);
    }

    @Override
    public INetworkPacket copy() {
        return new PingPacket(originatorIP, destinationIP, sendStartTime);
    }


}
