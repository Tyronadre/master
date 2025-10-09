package de.tyro.mcnetwork.gui.networkBook;

import de.tyro.mcnetwork.networkBook.data.SubTopic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.Vec2;

/**
 * Visual tile for a Subtopic.
 * - Draw icon area
 * - Draw label below
 * - Has a completion state (colored border if completed)
 */
public class SubtopicTile {
    public static final int BACKGROUND_COLOR = 0xFF222228;
    public static final int COMPLETED_COLOR = 0xFF222238;

    private final int x, y, w, h;
    private final SubTopic subtopic;
    private final Font font;

    public SubtopicTile(Vec2 pos, int w, int h, SubTopic subtopic) {
        this.x = (int) pos.x;
        this.y = (int) pos.y;
        this.w = w;
        this.h = h;
        this.subtopic = subtopic;
        this.font = Minecraft.getInstance().font;
    }

    public void render(GuiGraphics gg, double mouseXLocal, double mouseYLocal) {
        gg.fill(x, y, x + w, y + h, subtopic.isCompleted() ? COMPLETED_COLOR : BACKGROUND_COLOR);

        if (subtopic.getIcon() == null) gg.fill(x + 16, y + 8, w - 32, h - 32, BACKGROUND_COLOR);
        else gg.blit(subtopic.getIcon(), x + 2, y + 2, w - 4, h - 4, 0, 0, w - 4, h - 4, w - 4, h - 4);

        var split = font.getSplitter().splitLines(subtopic.getTitle(), w - 4, Style.EMPTY);
        if (split.size() > 3) split = split.subList(0, 3);
        int i = split.size();
        if (i > 1) for (var line : split) {
            gg.drawCenteredString(font, line.getString(), x + w / 2, y + h - font.lineHeight * i--, NetworkBookScreen.TEXT_COLOR);
        }
        else {
            gg.drawCenteredString(font, subtopic.getTitle(), x + w / 2, y + h - font.lineHeight * 2, NetworkBookScreen.TEXT_COLOR);
        }
    }

    public boolean isMouseOver(double localX, double localY) {
        return localX >= x && localX <= x + w && localY >= y && localY <= y + h;
    }

    public int getCenterX() {
        return x + w / 2;
    }

    public int getCenterY() {
        return y + h / 2;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    public SubTopic getSubtopic() {
        return subtopic;
    }
}
