package de.tyro.mcnetwork.gui.networkBook;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.tyro.mcnetwork.networkBook.data.SubTopic;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class DraggablePlane {

    public static final int CONNECTION_COLOR = 0x2FF24466;

    private final int x, y, width, height;
    private final Map<SubTopic, SubtopicTile> tiles = new HashMap<>();
    private float offsetX = 0, offsetY = 0; // pan offset
    private float lastDragMouseX, lastDragMouseY;
    private boolean dragging = false;
    private Consumer<SubtopicTile> onTileClicked;
    private final RenderType renderType;

    public DraggablePlane(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.renderType = RenderType.create(
                "gui_2",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                786432,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_GUI_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .createCompositeState(false)
        );
    }

    public void setSubtopics(List<SubTopic> subs) {
        tiles.clear();
        for (SubTopic s : subs) {
            tiles.put(s, new SubtopicTile(s.getPosition(), 80, 80, s));
        }
    }

    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks, Consumer<SubtopicTile> onTileClicked) {
        this.onTileClicked = onTileClicked;
        gg.fill(x, y, x + width, y + height, 0xFF111216);

        gg.pose().pushPose();
        gg.pose().translate(x + offsetX, y + offsetY, 0);

        // draw connections between tile centers
        for (var destination : tiles.entrySet()) {
            var dTile = destination.getValue();
            for (var origin : destination.getKey().getPrerequisite()) {
                var oTile = tiles.get(origin);
                VertexConsumer vertexconsumer = gg.bufferSource().getBuffer(renderType);
                var pose = gg.pose().last().pose();
                vertexconsumer.addVertex(pose, 100+ 10, 100 + 10, 0).setColor(CONNECTION_COLOR);
                vertexconsumer.addVertex(pose, 100 , 100 , 0).setColor(CONNECTION_COLOR);
                vertexconsumer.addVertex(pose, 200 -10, 200-10, 0).setColor(CONNECTION_COLOR);
                vertexconsumer.addVertex(pose, 200, 200, 0).setColor(CONNECTION_COLOR);
                gg.flush();
//                drawLine(gg, 100, 100, 150, 150, CONNECTION_COLOR);
            }
        }

        // draw tiles
        for (SubtopicTile t : tiles.values()) {
            t.render(gg, mouseX - x - offsetX, mouseY - y - offsetY);
        }

        gg.pose().popPose();
    }

    private void drawConnection(GuiGraphics gg, int x1, int y1, int x2, int y2) {
        Matrix4f pose = gg.pose().last().pose();
        float dx = x2 - x1;
        float dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        if (len == 0) return;

        // normalised perpendicular for line width (4px here)
        float nx = (float) (-dy / len) * 2f;
        float ny = (float) (dx / len) * 2f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        buffer.addVertex(pose, x1 + nx, y1 + ny, 0).setColor(CONNECTION_COLOR);
        buffer.addVertex(pose, x1 - nx, y1 - ny, 0).setColor(CONNECTION_COLOR);
        buffer.addVertex(pose, x2 - nx, y2 - ny, 0).setColor(CONNECTION_COLOR);
        buffer.addVertex(pose, x2 + nx, y2 + ny, 0).setColor(CONNECTION_COLOR);


        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
    }


    public boolean mouseClicked(double mx, double my, int button) {
        // check click on tiles (transform coords to plane)
        float localX = (float) (mx - x - offsetX);
        float localY = (float) (my - y - offsetY);
        for (SubtopicTile t : tiles.values()) {
            if (t.isMouseOver(localX, localY)) {
                if (onTileClicked != null) onTileClicked.accept(t);
                return true;
            }
        }
        // start dragging
        dragging = true;
        lastDragMouseX = (float) mx;
        lastDragMouseY = (float) my;
        return true;
    }

    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (!dragging) return false;
        float dx = (float) (mx - lastDragMouseX);
        float dy = (float) (my - lastDragMouseY);
        lastDragMouseX = (float) mx;
        lastDragMouseY = (float) my;
        offsetX += dx;
        offsetY += dy;
        clampOffsets();
        return true;
    }

    private void clampOffsets() {
        // clamp so plane cannot be dragged to show empty space:
        // compute content bounds
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (SubtopicTile t : tiles.values()) {
            minX = Math.min(minX, t.getX());
            minY = Math.min(minY, t.getY());
            maxX = Math.max(maxX, t.getX() + t.getWidth());
            maxY = Math.max(maxY, t.getY() + t.getHeight());
        }
        if (minX == Integer.MAX_VALUE) return; // no tiles

        // allowed range so that at least part of content visible
        float minOffsetX = Math.min(0, width - (maxX + 32));
        float maxOffsetX = Math.max(0, -minX + 16);
        float minOffsetY = Math.min(0, height - (maxY + 32));
        float maxOffsetY = Math.max(0, -minY + 16);

        if (offsetX < minOffsetX) offsetX = minOffsetX;
        if (offsetX > maxOffsetX) offsetX = maxOffsetX;
        if (offsetY < minOffsetY) offsetY = minOffsetY;
        if (offsetY > maxOffsetY) offsetY = maxOffsetY;
    }
}
