package de.tyro.mcnetwork.routing.packet;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public interface IPacketRenderable {

    void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float alpha);
}
