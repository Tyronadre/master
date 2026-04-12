package de.tyro.mcnetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ComputerBlockEntityRenderer implements BlockEntityRenderer<ComputerBlockEntity> {
    private static final double MAX_DISTANCE = 16.0;
    private static final double FADE_START_DISTANCE = 12.0;
    private static final Logger log = LogUtils.getLogger();

    public ComputerBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(ComputerBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        Vec3 blockCenter = Vec3.atCenterOf(be.getBlockPos());
        double distanceSq = player.distanceToSqr(blockCenter);

        if (distanceSq > MAX_DISTANCE * MAX_DISTANCE) return;
        if (!RenderUtil.isPlayerLookingAtBlock(player, be.getBlockPos(), MAX_DISTANCE)) return;

        float alpha = RenderUtil.computeFadeAlpha(Math.sqrt(distanceSq), FADE_START_DISTANCE, MAX_DISTANCE);
        if (alpha <= 0.05f) return;

        var eyePos = player.getEyePosition();
        var hudPos = eyePos.subtract(blockCenter).normalize().scale(1);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.translate(hudPos.x, hudPos.y, hudPos.z);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.XN.rotationDegrees(180.0F));
        poseStack.scale(0.025f, 0.025f, 0.025f);


        renderHud(be, poseStack, buffer, mc, packedLight, alpha);

        poseStack.popPose();
    }


    private void renderHud(ComputerBlockEntity be, PoseStack poseStack, MultiBufferSource buffer, Minecraft mc, int packedLight, float alpha) {
        if (be.getRoutingProtocol() == null) return;
        var renderer = new RenderUtil(poseStack, buffer, alpha, packedLight);

        var routingProtocolSize = be.getRoutingProtocol().getRenderSize(renderer.getFont());
        if (routingProtocolSize == null) return;

        var width = Math.max(routingProtocolSize.x, renderer.getFont().width(be.getRoutingProtocol().getClass().getSimpleName() + " @ " + be.getIP()));
        var height = routingProtocolSize.y;


        poseStack.translate(0, -height/2, 0);
        renderer.renderBackgroundQuad(width, height);

        renderer.drawStringWithAlphaColor(RenderUtil.Align.CENTER, be.getRoutingProtocol().getClass().getSimpleName() + " @ " + be.getIP(), width, 0);
        renderer.renderHLineWithAlphaColor(width, 9);
        renderer.renderHLineWithAlphaColor(width, 10);

        poseStack.translate(0, 14, 0);


        try {
            be.getRoutingProtocol().render(renderer);
        } catch (Exception e) {
            log.error("Error while rendering computer block entity", e);
        }

    }
}




