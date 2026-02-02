package de.tyro.mcnetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

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
        if (!isPlayerLookingAtBlock(player, be.getBlockPos(), MAX_DISTANCE)) return;

        float alpha = computeFadeAlpha((float) Math.sqrt(distanceSq));
        if (alpha <= 0.01f) return;

        renderHud(be, poseStack, buffer, mc, packedLight, alpha);
    }

    private float computeFadeAlpha(float distance) {
        if (distance <= FADE_START_DISTANCE) return 1.0f;
        if (distance >= MAX_DISTANCE) return 0.0f;

        float range = (float) (MAX_DISTANCE - FADE_START_DISTANCE);
        return 1.0f - ((distance - (float) FADE_START_DISTANCE) / range);
    }

    private boolean isPlayerLookingAtBlock(LocalPlayer player, BlockPos pos, double maxDistance) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getLookAngle();
        Vec3 reachVec = eyePos.add(lookVec.scale(maxDistance));

        ClipContext ctx = new ClipContext(eyePos, reachVec, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);

        BlockHitResult hit = player.level().clip(ctx);

        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
    }

    private void renderBackgroundQuad(PoseStack poseStack, MultiBufferSource buffer, float width, float height, float alpha) {
        float padding = 2f;
        float bgAlpha = alpha * 0.6f;

        float r = 0f;
        float g = 0f;
        float b = 0f;

        VertexConsumer vc = buffer.getBuffer(RenderType.debugQuads());

        Matrix4f mat = poseStack.last().pose();

        float x1 = -width / 2f - padding;
        float x2 = width / 2f + padding;
        float y1 = -padding;
        float y2 = height + padding;

        vc.addVertex(mat, x1, y1, 0).setColor(r, g, b, bgAlpha);
        vc.addVertex(mat, x1, y2, 0).setColor(r, g, b, bgAlpha);
        vc.addVertex(mat, x2, y2, 0).setColor(r, g, b, bgAlpha);
        vc.addVertex(mat, x2, y1, 0).setColor(r, g, b, bgAlpha);
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

        renderBackgroundQuad(poseStack, buffer, textWidth, text.size() * 10, alpha);

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




