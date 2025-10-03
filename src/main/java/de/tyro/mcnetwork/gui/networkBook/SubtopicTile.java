package de.tyro.mcnetwork.gui.networkBook;

import de.tyro.mcnetwork.networkBook.data.Subtopic;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Visual tile for a Subtopic.
 * - Draw icon area
 * - Draw label below
 * - Has a completion state (colored border if completed)
 */
public class SubtopicTile {

    private final int x, y, w, h;
    private final Subtopic subtopic;
    private boolean completed = false;

    public SubtopicTile(int x, int y, int w, int h, Subtopic subtopic) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.subtopic = subtopic;
    }

    public void render(GuiGraphics gg, double mouseXLocal, double mouseYLocal) {
        // tile background
        gg.fill(x, y, x + w, y + h, 0xFF222228);

        // icon area - placeholder box
        gg.fill(x + 8, y + 8, x + w - 8, y + h - 28, 0xFF2A6E2A);

        // title
        gg.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, subtopic.getTitle(), x + w/2, y + h - 20, 0xFFFFFF);

        // border if completed
        if (completed) {
            gg.drawString(net.minecraft.client.Minecraft.getInstance().font, "\u2713", x + w - 18, y + 8, 0xFF66FF66);
        }
    }

    public boolean isMouseOver(double localX, double localY) {
        return localX >= x && localX <= x + w && localY >= y && localY <= y + h;
    }

    public int getCenterX() { return x + w/2; }
    public int getCenterY() { return y + h/2; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return w; }
    public int getHeight() { return h; }

    public Subtopic getSubtopic() { return subtopic; }

    public void setCompleted(boolean c) { this.completed = c; }
}
