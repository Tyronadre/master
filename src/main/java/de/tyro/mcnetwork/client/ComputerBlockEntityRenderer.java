package de.tyro.mcnetwork.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ComputerBlockEntityRenderer implements BlockEntityRenderer<ComputerBlockEntity> {
    private static final double MAX_DISTANCE = 16.0;
    private static final double FADE_START_DISTANCE = 12.0;

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

        renderHud(be, poseStack, buffer, mc, packedLight, alpha);
    }


    private void renderHud(ComputerBlockEntity be, PoseStack poseStack, MultiBufferSource buffer, Minecraft mc, int packedLight, float alpha) {
        poseStack.pushPose();

        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.XN.rotationDegrees(180.0F));
        poseStack.scale(0.025f, 0.025f, 0.025f);

        Font font = mc.font;
        List<String> text = be.getRenderText();
        float textWidth = text.stream().mapToInt(font::width).max().orElse(100);

        RenderUtil.renderBackgroundQuad(poseStack, buffer, textWidth, text.size() * 10, alpha);

        poseStack.translate(0, 0, -0.01f);

        int textColor = ((int) (alpha * 255) << 24) | 0xFFFFFF;

        List<String> lines = be.getRenderText();

        int y = 0;
        for (String line : lines) {
            font.drawInBatch(line, -font.width(line) / 2f, y, textColor, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, packedLight);
            y += 10;
        }

        poseStack.popPose();
    }
}




