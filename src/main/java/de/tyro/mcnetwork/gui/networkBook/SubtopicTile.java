package de.tyro.mcnetwork.gui.networkBook;

import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.networkBook.data.SubTopic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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

    public void render(RenderUtil renderer, double mouseX, double mouseY, boolean highlight) {
        if (!subtopic.isShown()) return;

        //background
        var x2 = x + getWidth();
        var y2 = y + getHeight();
        renderer.fillRectangle(x, y, x2, y2, subtopic.isCompleted() ? COMPLETED_COLOR : BACKGROUND_COLOR);

        //highlight border
        if (highlight || isMouseOver(mouseX, mouseY)) {
            renderer.drawLine(x,y,x2,y, 0xFFFFFFFF,10);
            renderer.drawRectangle(x, y, x2, y2, RenderUtil.Color.GREEN.value, 20);
        }

        //icon
        if (subtopic.getIcon() == null)
            renderer.fillRectangle(x + 16, y + 8, w - 32, h - 32, BACKGROUND_COLOR);
        else
            renderer.blit(subtopic.getIcon(), x + 2, y + 2, w - 4, h - 4, 0, 0, w - 4, h - 4, w - 4, h - 4);

        //text
        var split = font.getSplitter().splitLines(subtopic.getTitle(), w - 4, Style.EMPTY);
        if (split.size() > 3) split = split.subList(0, 3);
        int i = split.size();
        if (i > 1) for (var line : split) renderer.drawString(RenderUtil.Align.CENTER, line.getString(), x + w / 2, y + h - font.lineHeight * i--, NetworkBookScreen.TEXT_COLOR);
        else renderer.drawString(RenderUtil.Align.CENTER, subtopic.getTitle(), x + w / 2, y + h - font.lineHeight * 2, NetworkBookScreen.TEXT_COLOR);

    }

    public boolean isMouseOver(double mX, double mY) {
        return mX >= x && mX <= x + w && mY >= y && mY <= y + h;
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
