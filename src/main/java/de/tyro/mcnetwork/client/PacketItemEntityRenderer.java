package de.tyro.mcnetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.tyro.mcnetwork.item.entity.PacketItemEntity;
import de.tyro.mcnetwork.routing.InFlightPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PacketItemEntityRenderer extends EntityRenderer<PacketItemEntity> {
    private static final double MAX_DISTANCE = 32.0;
    private static final double FADE_START_DISTANCE = 12.0;

    public PacketItemEntityRenderer(Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(PacketItemEntity entity) {
        return null;
    }


    @Override
    public void render(PacketItemEntity p_entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(p_entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        if (p_entity instanceof PacketItemEntity pie) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;

            Vec3 center = p_entity.getEyePosition();
            double distanceSq = player.distanceToSqr(center);

            if (distanceSq > MAX_DISTANCE * MAX_DISTANCE) return;

            float alpha = RenderUtil.computeFadeAlpha(Math.sqrt(distanceSq), FADE_START_DISTANCE, MAX_DISTANCE);
            if (alpha <= 0.05f) return;

            renderHud(pie.getInFlightPacket(), poseStack, bufferSource, Minecraft.getInstance(), packedLight, alpha);
        }
    }

    private void renderHud(InFlightPacket packet, PoseStack poseStack, MultiBufferSource buffer, Minecraft mc, int packedLight, float alpha) {
        poseStack.pushPose();

        poseStack.translate(0,0.5,0); //render above
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.XN.rotationDegrees(180.0F));
        poseStack.scale(0.025f, 0.025f, 0.025f);

        Font font = mc.font;
        List<String> text = packet.packet.getRenderContent();
        float textWidth = text.stream().mapToInt(font::width).max().orElse(100);

        RenderUtil.renderBackgroundQuad(poseStack, buffer, textWidth, text.size() * 10, alpha);

        poseStack.translate(0, 0, -0.01f);

        int y = 0;
        for (String line : text) {
            RenderUtil.drawString(line, -font.width(line) / 2f, y, alpha, poseStack, buffer, packedLight);
            y += 10;
        }

        poseStack.popPose();
    }
}