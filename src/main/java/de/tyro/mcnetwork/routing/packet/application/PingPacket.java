package de.tyro.mcnetwork.routing.packet.application;


import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.entity.NetworkFrameEntity;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.routing.packet.IApplicationPacket;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec2;

import java.util.StringJoiner;
import java.util.UUID;

public class PingPacket extends NetworkPacket implements IApplicationPacket {

    public long sendStartTime;

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
                .add("sourceIp=" + getOriginatorIP())
                .add("destinationIp=" + getDestinationIP())
                .toString();
    }

    @Override
    public Vec2 getRenderSize(Font font) {
        var line = "Time " + (getSimulationEngine().getSimTime() - sendStartTime) + "ms";
        return new Vec2(font.width(line), font.lineHeight);
    }

    @Override
    protected void renderPacketContent(RenderUtil renderer, PoseStack poseStack, float width) {
        String line = "Time " + (getSimulationEngine().getSimTime() - sendStartTime) + "ms";
        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, line,  width, 0);
    }

    @Override
    public INetworkPacket copy() {
        return new PingPacket(getOriginatorIP(), getDestinationIP(), sendStartTime);
    }

}
