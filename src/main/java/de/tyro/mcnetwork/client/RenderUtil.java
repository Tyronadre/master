package de.tyro.mcnetwork.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class RenderUtil {

    private float alpha;
    private float width;

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public Font getFont() {
        return font;
    }

    public void setPose(PoseStack pose) {
        this.poseStack = pose;
    }

    public void renderItem(Item item, int x, int y, float scale) {
        var stack = new ItemStack(item, 1);
        var model = mc.getItemRenderer().getModel(stack, null, null, 0);

        poseStack.pushPose();
        poseStack.translate(x + scale / 2, y + scale / 2, 0);

        try {
            poseStack.scale(scale, -scale, 0);
            boolean flag = !model.usesBlockLight();
            if (flag) {
                Lighting.setupForFlatItems();
            }

            mc.getItemRenderer()
                    .render(stack, ItemDisplayContext.GUI, false, poseStack, this.buffer, 15728880, OverlayTexture.NO_OVERLAY, model);

            RenderSystem.disableDepthTest();
            mc.renderBuffers().bufferSource().endBatch();
            RenderSystem.enableDepthTest();

            if (flag) {
                Lighting.setupFor3DItems();
            }
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering item");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Item being rendered");
            crashreportcategory.setDetail("Item Type", () -> String.valueOf(stack.getItem()));
            crashreportcategory.setDetail("Item Components", () -> String.valueOf(stack.getComponents()));
            crashreportcategory.setDetail("Item Foil", () -> String.valueOf(stack.hasFoil()));
            throw new ReportedException(crashreport);
        }

        poseStack.popPose();

    }

    public enum Align {
        LEFT,
        RIGHT,
        CENTER
    }

    public enum Color {
        BLACK(0xFFFFFFFF),
        RED(0xFFFF0000),
        GREEN(0xFF00FF00),
        BLUE(0xFF0000FF),
        MAGENTA(0xFFf400f4),
        WHITE(0xFFFFFFFF),
        YELLOW(0xFFFFFF00);

        public final int value;

        Color(int value) {
            this.value = value;
        }

    }

    private final Minecraft mc;
    private Font font;
    private PoseStack poseStack;
    private final MultiBufferSource buffer;
    private final int packedLight;
    private final GuiGraphics guiGraphics;

    private boolean clipEnabled;


    public RenderUtil(PoseStack poseStack, MultiBufferSource buffer, float alpha, int packedLight) {
        this.mc = Minecraft.getInstance();
        this.font = mc.font;
        this.poseStack = poseStack;
        this.buffer = buffer;
        this.alpha = alpha;
        this.packedLight = packedLight;
        this.guiGraphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void renderHLineWithAlphaColor(float width, float y) {
        var color = getTextColorFromAlpha();

        VertexConsumer vc = buffer.getBuffer(RenderType.debugLineStrip(1));
        Matrix4f mat = poseStack.last().pose();

        vc.addVertex(mat, -width / 2F, y, 0).setColor(color);
        vc.addVertex(mat, width / 2F, y, 0).setColor(color);
    }

    static float computeFadeAlpha(double distance, double fadeStart, double maxDistance) {
        if (distance <= fadeStart) return 1.0f;
        if (distance >= maxDistance) return 0.0f;

        return (float) (1.0f - ((distance - fadeStart) / (maxDistance - fadeStart)));
    }

    static boolean isPlayerLookingAtBlock(LocalPlayer player, BlockPos pos, double maxDistance) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getLookAngle();
        Vec3 reachVec = eyePos.add(lookVec.scale(maxDistance));

        ClipContext ctx = new ClipContext(eyePos, reachVec, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);

        BlockHitResult hit = player.level().clip(ctx);

        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
    }

    public void renderBackgroundQuad(float width, float height) {
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

    public void renderBackgroundQuadWithColor(float width, float height, int color) {
        float padding = 2f;
        float bgAlpha = alpha * 0.6f;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

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

    public void drawLine(float x1, float y1, float x2, float y2, int color, float width) {
        if (width <= 1f) {
            VertexConsumer vc = buffer.getBuffer(RenderType.debugLineStrip(1));
            Matrix4f mat = poseStack.last().pose();

            vc.addVertex(mat, x1, y1, 0).setColor(color);
            vc.addVertex(mat, x2, y2, 0).setColor(color);
            return;
        }

        float dx = x2 - x1;
        float dy = y2 - y1;

        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;

        // Normalize direction
        dx /= len;
        dy /= len;

        // Perpendicular (normal)
        float nx = -dy;
        float ny = dx;

        float halfW = width / 2f;

        // Offset vector
        float ox = nx * halfW;
        float oy = ny * halfW;

        Matrix4f mat = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.DEBUG_QUADS);

        vc.addVertex(mat, x1 - ox, y1 - oy, 0).setColor(color);
        vc.addVertex(mat, x1 + ox, y1 + oy, 0).setColor(color);
        vc.addVertex(mat, x2 + ox, y2 + oy, 0).setColor(color);
        vc.addVertex(mat, x2 - ox, y2 - oy, 0).setColor(color);
    }

    public void drawLine(float x1, float y1, float x2, float y2, int color) {
        drawLine(x1, y1, x2, y2, color, 1);
    }

    public void fillRectangle(float x1, float y1, float x2, float y2, int color) {
        VertexConsumer vc = buffer.getBuffer(RenderType.debugQuads());
        Matrix4f mat = poseStack.last().pose();

        vc.addVertex(mat, x1, y1, 0).setColor(color);
        vc.addVertex(mat, x1, y2, 0).setColor(color);
        vc.addVertex(mat, x2, y2, 0).setColor(color);
        vc.addVertex(mat, x2, y1, 0).setColor(color);
    }

    public void drawRectangle(float x1, float y1, float x2, float y2, int color, int width) {
        ((MultiBufferSource.BufferSource) buffer).endBatch();
        var offset = (float) width / 2;
        drawLine(x2, y1 + offset, x2, y2 - offset, color, width);
        drawLine(x2 + offset, y2, x1 - offset, y2, color, width);
        drawLine(x1, y2 - offset, x1, y1 + offset, color, width);
        drawLine(x1 - offset, y1, x2 + offset, y1, color, width);
    }

    public int getTextColorFromAlpha() {
        return ((int) (alpha * 255) << 24) | 0xFFFFFF;
    }


    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }


    /**
     * Draws a string
     *
     * @param line   the string
     * @param color  the color
     * @param x      the x origin where the string will be drawn (left side of the string)
     * @param y      the y origin where the string will be drawn (top side of the string)
     * @param shadow if the string should have a shadow
     */
    public void drawString(String line, int color, float x, float y, boolean shadow) {
        Minecraft.getInstance().font.drawInBatch(line, x, y, color, shadow, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, packedLight);
    }

    /**
     * Draws an aligned string with shadow
     *
     * @param align where it should be aligned
     * @param line  the string
     * @param color the color
     * @param width the width which it will be aligned to
     * @param y     the y coordinate (height)
     */
    public void drawStringWithShadow(Align align, String line, int color, float width, float y) {
        drawString(align, line, color, width, y, true);
    }

    /**
     * Draws an aligned string without shadow
     *
     * @param align where it should be aligned
     * @param line  the string
     * @param color the color
     * @param width the width which it will be aligned to
     * @param y     the y coordinate (height)
     */
    public void drawString(Align align, String line, int color, float width, float y) {
        drawString(align, line, color, width, y, false);
    }

    /**
     * Draws an aligned string
     *
     * @param align  where it should be aligned
     * @param line   the string
     * @param color  the color
     * @param width  the width which it will be aligned to
     * @param y      the y coordinate (height)
     * @param shadow if the string should have a shadow
     */
    public void drawString(Align align, String line, int color, float width, float y, boolean shadow) {
        var font = Minecraft.getInstance().font;

        float x = switch (align) {
            case LEFT -> -width / 2;
            case CENTER -> -font.width(line) / 2F;
            case RIGHT -> width / 2 - font.width(line);
        };

        drawString(line, color, x, y, shadow);
    }

    /**
     * Draws an aligned string without shadow with a grayscale color according to the current alpha value of the renderer
     *
     * @param align where it should be aligned
     * @param line  the string
     * @param width the width which it will be aligned to
     * @param y     the y coordinate (height)
     */
    public void drawStringWithAlphaColor(Align align, String line, float width, float y) {
        drawString(align, line, getTextColorFromAlpha(), width, y, false);
    }

    /**
     * Draws each element of the collection as a string, up to maxWidth elements, aligned and colored.
     * If there are more elements, draws "..." to indicate truncation.
     *
     * @param align      the alignment for each string
     * @param collection the collection of elements to draw
     * @param maxWidth   the maximum number of elements to draw
     * @param color      the color for the strings
     * @param width      the width for alignment
     * @param y          the starting y coordinate
     */
    public <T extends Collection<?>> void drawCollectionAsString(Align align, T collection, int maxWidth, int color, float width, float y) {
        var iterator = collection.iterator();
        int currentWidth = 0;
        StringBuilder string = new StringBuilder("[");
        while (iterator.hasNext()) {
            var element = iterator.next();
            String str = element.toString();
            if (!iterator.hasNext()) {
                if (currentWidth + font.width(str) + font.width("]") > maxWidth) string.append(", ...");
                else string.append(str);
            } else {
                if (font.width(str) + currentWidth + font.width(", ...")>= maxWidth) {
                    string.append(", ...");
                    break;
                } else string.append(str).append(", ");
                currentWidth += font.width(str + ", ");
            }
        }
        string.append("]");
        drawString(align, string.toString(), color, width, y, false);
    }


    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given position and dimensions with texture coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x             the x-coordinate of the top-left corner of the blit
     *                      position.
     * @param y             the y-coordinate of the top-left corner of the blit
     *                      position.
     * @param width         the width of the blitted portion.
     * @param height        the height of the blitted portion.
     * @param uOffset       the horizontal texture coordinate offset.
     * @param vOffset       the vertical texture coordinate offset.
     * @param uWidth        the width of the blitted portion in texture coordinates.
     * @param vHeight       the height of the blitted portion in texture coordinates.
     * @param textureWidth  the width of the texture.
     * @param textureHeight the height of the texture.
     */
    public void blit(
            ResourceLocation atlasLocation,
            int x,
            int y,
            int width,
            int height,
            float uOffset,
            float vOffset,
            int uWidth,
            int vHeight,
            int textureWidth,
            int textureHeight
    ) {
        this.blit(
                atlasLocation, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight
        );
    }

    /**
     * Performs the inner blit operation for rendering a texture with the specified coordinates and texture coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x1            the x-coordinate of the first corner of the blit position.
     * @param x2            the x-coordinate of the second corner of the blit position
     *                      .
     * @param y1            the y-coordinate of the first corner of the blit position.
     * @param y2            the y-coordinate of the second corner of the blit position
     *                      .
     * @param blitOffset    the z-level offset for rendering order.
     * @param uWidth        the width of the blitted portion in texture coordinates.
     * @param vHeight       the height of the blitted portion in texture coordinates.
     * @param uOffset       the horizontal texture coordinate offset.
     * @param vOffset       the vertical texture coordinate offset.
     * @param textureWidth  the width of the texture.
     * @param textureHeight the height of the texture.
     */
    public void blit(
            ResourceLocation atlasLocation,
            int x1,
            int x2,
            int y1,
            int y2,
            int blitOffset,
            int uWidth,
            int vHeight,
            float uOffset,
            float vOffset,
            int textureWidth,
            int textureHeight
    ) {
        this.innerBlit(
                atlasLocation,
                x1,
                x2,
                y1,
                y2,
                blitOffset,
                (uOffset + 0.0F) / (float) textureWidth,
                (uOffset + (float) uWidth) / (float) textureWidth,
                (vOffset + 0.0F) / (float) textureHeight,
                (vOffset + (float) vHeight) / (float) textureHeight
        );
    }

    void innerBlit(
            ResourceLocation atlasLocation,
            int x1,
            int x2,
            int y1,
            int y2,
            int blitOffset,
            float minU,
            float maxU,
            float minV,
            float maxV
    ) {
        //finish whatever we were drawing up till now, otherwise the order might be fked
        ((MultiBufferSource.BufferSource) buffer).endBatch();

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = poseStack.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.addVertex(matrix4f, (float) x1, (float) y1, (float) blitOffset).setUv(minU, minV);
        bufferbuilder.addVertex(matrix4f, (float) x1, (float) y2, (float) blitOffset).setUv(minU, maxV);
        bufferbuilder.addVertex(matrix4f, (float) x2, (float) y2, (float) blitOffset).setUv(maxU, maxV);
        bufferbuilder.addVertex(matrix4f, (float) x2, (float) y1, (float) blitOffset).setUv(maxU, minV);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
    }

}
