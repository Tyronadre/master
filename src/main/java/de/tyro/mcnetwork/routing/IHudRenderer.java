package de.tyro.mcnetwork.routing;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public interface IHudRenderer {

    void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float alpha);
}
