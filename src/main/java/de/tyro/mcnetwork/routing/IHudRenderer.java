package de.tyro.mcnetwork.routing;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec2;

public interface IHudRenderer {

    void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float alpha);

    Vec2 getRenderSize(Font font);
}
