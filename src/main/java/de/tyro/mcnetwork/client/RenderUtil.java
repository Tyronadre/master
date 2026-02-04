package de.tyro.mcnetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class RenderUtil {

    static float computeFadeAlpha(double distance, double fadeStart, double maxDistance) {
        if (distance <= fadeStart) return 1.0f;
        if (distance >= maxDistance) return 0.0f;

        return (float) (1.0f - ((distance - fadeStart) /  (maxDistance - fadeStart)));
    }

    static boolean isPlayerLookingAtBlock(LocalPlayer player, BlockPos pos, double maxDistance) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getLookAngle();
        Vec3 reachVec = eyePos.add(lookVec.scale(maxDistance));

        ClipContext ctx = new ClipContext(eyePos, reachVec, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);

        BlockHitResult hit = player.level().clip(ctx);

        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
    }

    static void renderBackgroundQuad(PoseStack poseStack, MultiBufferSource buffer, float width, float height, float alpha) {
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

    public static int getTextColorFromAlpha(float alpha) {
        return ((int) (alpha * 255) << 24) | 0xFFFFFF;
    }

    public static void drawString(String line, float x, int y, float alpha, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Minecraft.getInstance().font.drawInBatch(line, x, y, getTextColorFromAlpha(alpha), false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, packedLight);
    }
}
