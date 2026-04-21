package de.tyro.mcnetwork.simulation.packet.application;


import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.SimulationEngine;
import de.tyro.mcnetwork.simulation.packet.IApplicationPacket;
import de.tyro.mcnetwork.simulation.packet.INetworkPacket;
import de.tyro.mcnetwork.simulation.packet.NetworkPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec2;

import java.util.StringJoiner;
import java.util.UUID;

public class PingPacket extends NetworkPacket implements IApplicationPacket {

    public long sendStartTime;
    public long serverTime;
    private long offset;

    public PingPacket(IP src, IP dst, long sendStartTime) {
        super(src, dst);
        this.sendStartTime = sendStartTime;
    }

    public PingPacket(UUID uuid, IP src, IP dst, long sendStartTime, long serverTime) {
        super(uuid, src, dst);
        this.sendStartTime = sendStartTime;
        this.serverTime = serverTime;
        this.offset = SimulationEngine.getInstance(true).getSimTime() - serverTime;
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
        var superSize = super.getRenderSize(font);
        var line = "Time " + (getSimulationEngine().getSimTime() - sendStartTime - offset) + "ms";
        return new Vec2(Math.max(superSize.x, font.width(line)), superSize.y + font.lineHeight);
    }

    @Override
    protected void renderPacketContent(RenderUtil renderer, PoseStack poseStack, float width) {
        String line = "Time " + (SimulationEngine.getInstance(true).getSimTime() - sendStartTime - offset) + "ms";
        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, line, width, 0);
    }

    @Override
    public INetworkPacket copy() {
        return new PingPacket(getId(), getOriginatorIP(), getDestinationIP(), sendStartTime, serverTime);
    }

}
