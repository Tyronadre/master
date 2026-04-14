package de.tyro.mcnetwork.networkBook.markdown.block;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ListBlock extends Block {
    public final List<List<InlineNode>> items;
    public final boolean ordered;

    public ListBlock(List<List<InlineNode>> items, boolean ordered) {
        this.items = items;
        this.ordered = ordered;
    }

    @Override
    public int render(GuiGraphics gg, int x, int y, int width, int h) {
        int curY = y;
        int i = 1;
        var spaces = (int) Math.floor(Math.log10(items.size())) * 5;
        for (var item : items) {
            if (gg != null) {
                gg.drawString(font, ordered ? Component.literal(i++ + ".") : Component.literal("•"), x, curY, 0xFFFFFF);
            } else {
                i++;
            }
            for (StyledLine sl : layoutInlineNodes(item, width - 10)) {
                if (gg != null) {
                    renderStyledLine(gg, sl, x + 10 + spaces, curY);
                }
                curY += lineHeight;
            }
        }
        return curY - y;
    }
}
