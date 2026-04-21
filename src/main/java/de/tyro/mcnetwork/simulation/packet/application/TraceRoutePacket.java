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

public class TraceRoutePacket extends NetworkPacket implements IApplicationPacket {

    public long offset;
    public long sendStartTime;

    public TraceRoutePacket(IP src, IP dst, long sendStartTime) {
        super(src, dst);
        this.sendStartTime = sendStartTime;
    }

    public TraceRoutePacket(UUID uuid, IP src, IP dst, long sendStartTime, long offset) {
        super(uuid, src, dst);
        this.sendStartTime = sendStartTime;
        this.offset = SimulationEngine.getInstance(true).getSimTime() - offset;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TraceRoutePacket.class.getSimpleName() + "[", "]")
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
        return new Vec2(Math.max(superSize.x, font.width(line)), font.lineHeight + superSize.y);
    }

    @Override
    protected void renderPacketContent(RenderUtil renderer, PoseStack poseStack, float width) {
        String line = "Time " + (getSimulationEngine().getSimTime() - sendStartTime - offset) + "ms";
        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, line, width, 0);
    }

    @Override
    public INetworkPacket copy() {
        return new TraceRoutePacket(getId(), getOriginatorIP(), getDestinationIP(), sendStartTime, offset);
    }

}

