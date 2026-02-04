package de.tyro.mcnetwork.routing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IPacketRenderable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

public class InFlightPacket implements IPacketRenderable {
    public final INetworkNode from;
    public final INetworkNode to;
    public final INetworkPacket packet;

    private double traveled = 0;

    public InFlightPacket(INetworkNode from, INetworkNode to, INetworkPacket packet) {
        this.from = from;
        this.to = to;
        this.packet = packet;
    }

    public boolean tick() {
        traveled += SimulationEngine.getInstance().getSimulationSpeed();
        return traveled >= from.getPos().distanceTo(to.getPos());
    }

    public Vec3 getCurrentPosition() {
        double total = from.getPos().distanceTo(to.getPos());
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

        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.CENTER, "UDP @ " + from.getIP().toString() + " -> " + to.getIP().toString(), alpha, 0, -15, poseStack, bufferSource, packedLight);

        poseStack.popPose();

        packet.render(poseStack, bufferSource, packedLight, alpha);

        poseStack.popPose();
    }
}