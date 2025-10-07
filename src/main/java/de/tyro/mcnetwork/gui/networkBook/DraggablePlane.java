package de.tyro.mcnetwork.gui.networkBook;

import de.tyro.mcnetwork.networkBook.data.SubTopic;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 2D Draggable Plane that contains SubtopicTile elements laid out in a grid.
 * Dragging pans the plane; we clamp the translate so user can't move to meaningless area.
 */
public class DraggablePlane {

    private final int x, y, width, height;
    private final List<SubtopicTile> tiles = new ArrayList<>();
    private float offsetX = 0, offsetY = 0; // pan offset
    private float lastDragMouseX, lastDragMouseY;
    private boolean dragging = false;
    private Consumer<SubtopicTile> onTileClicked;

    // layout parameters
    private final int tileW = 96, tileH = 96, spacingX = 24, spacingY = 24;

    public DraggablePlane(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public void setSubtopics(List<SubTopic> subs) {
        tiles.clear();
        // simple grid layout: compute rows/cols based on available width
        int cols = Math.max(1, (width - 32) / (tileW + spacingX));
        int idx = 0;
        int startX = 16;
        int startY = 16;
        for (SubTopic s : subs) {
            int col = idx % cols;
            int row = idx / cols;
            int tx = startX + col * (tileW + spacingX);
            int ty = startY + row * (tileH + spacingY);
            SubtopicTile t = new SubtopicTile(tx, ty, tileW, tileH, s);
            tiles.add(t);
            idx++;
        }
    }

    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks, Consumer<SubtopicTile> onTileClicked) {
        this.onTileClicked = onTileClicked;
        // background panel
        gg.fill(x, y, x + width, y + height, 0xFF111216);

        // save clip / scissor would be ideal; for blueprint we rely on drawing inside bounds
        gg.pose().pushPose();
        gg.pose().translate(x + offsetX, y + offsetY, 0);

        // draw connection lines (very simple: draw lines between sequential tiles)
        for (int i = 0; i < tiles.size() - 1; i++) {
            SubtopicTile a = tiles.get(i);
            SubtopicTile b = tiles.get(i + 1);
            int ax = a.getCenterX();
            int ay = a.getCenterY();
            int bx = b.getCenterX();
            int by = b.getCenterY();
            gg.fill(ax, ay - 1, bx, ay + 1, 0xFF555555);
        }

        // draw tiles
        for (SubtopicTile t : tiles) {
            t.render(gg, mouseX - x - offsetX, mouseY - y - offsetY);
        }

        gg.pose().popPose();
    }

    public boolean mouseClicked(double mx, double my, int button) {
        // check click on tiles (transform coords to plane)
        float localX = (float) (mx - x - offsetX);
        float localY = (float) (my - y - offsetY);
        for (SubtopicTile t : tiles) {
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
        for (SubtopicTile t : tiles) {
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

    public void refreshCompletionState() {
        // placeholder to refresh visual state from TopicManager
    }

    // getters for testing / hooks
    public List<SubtopicTile> getTiles() { return tiles; }
}
