package de.tyro.mcnetwork.networkBook.markdown.block;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class HeaderBlock extends Block {
    public final int level;
    public final String text;

    public HeaderBlock(int level, String text) {
        this.level = level;
        this.text = text;
    }

    @Override
    public int render(GuiGraphics gg, int x, int y, int width, int h) {
        //TODO different sizes
        List<StyledLine> lines = layoutInlineWords(text, width, ChatFormatting.BOLD, false);
        int curY = y;
        for (StyledLine sl : lines) {
            if (gg != null) {
                renderStyledLine(gg, sl, x, curY);
            }
            curY += lineHeight + 2;
        }
        return curY - y;
    }
}
