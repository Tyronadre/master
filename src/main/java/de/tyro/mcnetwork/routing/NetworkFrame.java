package de.tyro.mcnetwork.routing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IPacketRenderable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

public class NetworkFrame implements IPacketRenderable {
    private static final SimulationEngine sim = SimulationEngine.getInstance();
    public final double movementPerTick = 0.01;
    public final INetworkNode from;
    public final INetworkNode to;
    public final INetworkPacket packet;
    public final int ttl;

    private double lastTick;
    private double traveled = 0;
    private boolean arrived = false;

    public NetworkFrame(INetworkNode from, INetworkNode to, INetworkPacket packet, int ttl) {
        this.from = from;
        this.to = to;
        this.packet = packet;
        this.ttl = ttl;
        packet.setFrame(this);
        lastTick = sim.getExactSimTime();
    }

    public boolean tick() {
        traveled += movementPerTick * (sim.getExactSimTime() - lastTick);
        lastTick = sim.getExactSimTime();
        return hasArrived();
    }

    public boolean hasArrived() {
        if (!arrived) {
            arrived = traveled >= from.distanceTo(to);
        }
        return arrived;
    }

    public Vec3 getCurrentPosition() {
        double total = from.distanceTo(to);
        double t = Math.min(1.0, traveled / total);
        return from.getPos().lerp(to.getPos(), t);
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float alpha) {
        var mc = Minecraft.getInstance();

        poseStack.pushPose();

        poseStack.translate(0, 0.5, 0); //render above
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.XN.rotationDegrees(180.0F));
        poseStack.scale(0.025f, 0.025f, 0.025f);

        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);

        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.CENTER, "UDP @ " + from.getIP().toString() + " -> " + to.getIP().toString() + " TTL: " + ttl, alpha, 0, -15, poseStack, bufferSource, packedLight);

        poseStack.popPose();

        packet.render(poseStack, bufferSource, packedLight, alpha);

        poseStack.popPose();
    }

    public IP fromIP() {
        return from.getIP();
    }


}