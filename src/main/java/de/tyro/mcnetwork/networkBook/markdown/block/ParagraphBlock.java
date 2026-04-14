package de.tyro.mcnetwork.networkBook.markdown.block;

import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class ParagraphBlock extends Block {
    public final List<InlineNode> inlineNodes;

    public ParagraphBlock(List<InlineNode> nodes) {
        this.inlineNodes = nodes;
    }

    @Override
    public int render(GuiGraphics gg, int x, int y, int width, int h) {
        List<StyledLine> lines = layoutInlineNodes(inlineNodes, width);
        int curY = y;
        for (StyledLine sl : lines) {
            if (gg != null) {
                renderStyledLine(gg, sl, x, curY);
            }
            curY += lineHeight;
        }
        return curY - y;
    }
}
