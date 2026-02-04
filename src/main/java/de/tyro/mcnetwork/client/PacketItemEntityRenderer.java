package de.tyro.mcnetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.item.entity.PacketItemEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class PacketItemEntityRenderer extends EntityRenderer<PacketItemEntity> {
    private static final double MAX_DISTANCE = 32.0;
    private static final double FADE_START_DISTANCE = 12.0;

    public PacketItemEntityRenderer(Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull PacketItemEntity entity) {
        return ResourceLocation.withDefaultNamespace("paper");
    }


    @Override
    public void render(@NotNull PacketItemEntity packet, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(packet, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        Vec3 center = packet.getEyePosition();
        double distanceSq = player.distanceToSqr(center);

        if (distanceSq > MAX_DISTANCE * MAX_DISTANCE) return;

        float alpha = RenderUtil.computeFadeAlpha(Math.sqrt(distanceSq), FADE_START_DISTANCE, MAX_DISTANCE);
        if (alpha <= 0.05f) return;

        packet.getInFlightPacket().render(poseStack, bufferSource, packedLight, alpha);
    }
}