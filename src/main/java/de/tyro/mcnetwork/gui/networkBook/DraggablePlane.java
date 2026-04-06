package de.tyro.mcnetwork.gui.networkBook;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.networkBook.data.SubTopic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DraggablePlane {

    public static final int CONNECTION_COLOR = 0x2FF24466;

    private final int x, y, width, height;
    private final Map<SubTopic, SubtopicTile> tiles = new HashMap<>();
    private final NetworkBookScreen screen;
    private float offsetX = 0, offsetY = 0; // pan offset
    private float lastDragMouseX, lastDragMouseY;
    private boolean dragging = false;
    private final RenderUtil renderer;
    private SubtopicTile hoveredTile = null;

    public DraggablePlane(NetworkBookScreen screen, int x, int y, int width, int height) {
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.renderer = new RenderUtil(new PoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), 1, 0);

    }

    public void setSubtopics(List<SubTopic> subs) {
        tiles.clear();
        for (SubTopic s : subs) {
            tiles.put(s, new SubtopicTile(s.getPosition(), 80, 80, s));
        }
    }

    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        gg.fill(x, y, x + width, y + height, 0xFF111216);

        gg.pose().pushPose();
        gg.pose().translate(x + offsetX, y + offsetY, 0);
        renderer.setPose(gg.pose());

        List<SubtopicTile> shownTiles = tiles.values().stream().filter(t -> t.getSubtopic().isShown()).toList();

        hoveredTile = null;
        for (SubtopicTile t : shownTiles) {
            if (t.isMouseOver(mouseX - x - offsetX, mouseY - y - offsetY)) {
                hoveredTile = t;
                break;
            }
        }

        // draw connections between tile centers
        for (SubtopicTile dTile : shownTiles) {
            SubTopic dest = dTile.getSubtopic();
            boolean highlightLines = hoveredTile == dTile && !dest.isInteractable();
            for (SubTopic origin : dest.getPrerequisite()) {
                SubtopicTile oTile = tiles.get(origin);
                if (oTile != null) {
                    renderer.drawLine(oTile.getCenterX(), oTile.getCenterY(), dTile.getCenterX(), dTile.getCenterY(), CONNECTION_COLOR, highlightLines ? 8 : 4);
                }
            }
        }

        // draw tiles
        for (SubtopicTile t : shownTiles) {
            var highlight = hoveredTile != null && hoveredTile.getSubtopic().getPrerequisite().stream().anyMatch(it -> it.equals(t.getSubtopic()));

            t.render(renderer, mouseX - x - offsetX, mouseY - y - offsetY, highlight);
        }

        gg.pose().popPose();

        clampOffsets(shownTiles);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        // check click on tiles (transform coords to plane)
        float localX = (float) (mx - x - offsetX);
        float localY = (float) (my - y - offsetY);
        for (SubtopicTile t : tiles.values()) {
            if (t.isMouseOver(localX, localY) && isClickable(t.getSubtopic())) {
                screen.onSubtopicClicked(t.getSubtopic());
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

    private void clampOffsets(List<SubtopicTile> shownTiles) {
        // clamp so plane cannot be dragged to show empty space:
        // compute content bounds of shown tiles
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (SubtopicTile t : shownTiles) {
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

    private boolean isShown(SubTopic subtopic) {
        return subtopic.getPrerequisite().isEmpty() || subtopic.getPrerequisite().stream().anyMatch(p -> p.isCompleted());
    }

    private boolean isClickable(SubTopic subtopic) {
        return subtopic.getPrerequisite().isEmpty() || subtopic.getPrerequisite().stream().allMatch(p -> p.isCompleted());
    }

    private boolean hasUnfulfilledPrereqs(SubTopic subtopic) {
        return subtopic.getPrerequisite().stream().anyMatch(p -> !p.isCompleted());
    }
}
