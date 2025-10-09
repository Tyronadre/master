package de.tyro.mcnetwork.gui.networkBook.Scene3D;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

/**
 * Renders a small 3D scene inside the book GUI.
 * - Uses its own camera
 * - Animations run on client time (not game time)
 * - Fully interactive with mouse orbit + zoom
 */
public class Scene3DRenderer {

    private final Minecraft mc = Minecraft.getInstance();
    private final SceneCamera camera = new SceneCamera();
    private final SceneDefinition scene;
    private RenderTarget target;

    public Scene3DRenderer(SceneDefinition scene) {
        this.scene = scene;
    }

    public void init(int width, int height) {
        if (target == null || target.width != width || target.height != height) {

            target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            target.setClearColor(0f, 0f, 0f, 0f);
        }
    }

    private void renderSceneContents(int width, int height, float partialTicks) {

    }

    public void render(GuiGraphics gg, int x, int y, int width, int height, float partialTicks) {
        init(width, height);

        target.bindWrite(true);
        RenderSystem.clearColor(0, 0, 0, 0);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        Matrix4f projection = new Matrix4f()
                .perspective((float) Math.toRadians(45.0), (float) width / height, 0.05f, 100.0f);

        Matrix4f view = camera.getViewMatrix();

        RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(view);

        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        for (SceneDefinition.SceneBlock block : scene.blocks) {
            poseStack.pushPose();
            poseStack.translate(block.x(), block.y(), block.z());
            dispatcher.renderSingleBlock(block.state(), poseStack, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            poseStack.popPose();
        }

        scene.animator.renderPackets(poseStack, buffer, partialTicks);
        buffer.endBatch();

        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());

        Matrix4f pose = gg.pose().last().pose();
        BufferBuilder bb = new BufferBuilder(new ByteBufferBuilder(5), VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        bb.addVertex(pose, x, y + height, 0).setUv(0, 1);
        bb.addVertex(pose, x + width, y + height, 0).setUv(1, 1);
        bb.addVertex(pose, x + width, y, 0).setUv(1, 0);
        bb.addVertex(pose, x, y, 0).setUv(0, 0);

        BufferUploader.drawWithShader(bb.buildOrThrow());
    }

    public void handleMouseDrag(double dx, double dy) {
        camera.orbit((float)dx, (float)dy);
    }

    public void handleScroll(double delta) {
        camera.zoom((float)delta);
    }
}
