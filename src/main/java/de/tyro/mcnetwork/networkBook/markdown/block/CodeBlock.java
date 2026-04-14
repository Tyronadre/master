package de.tyro.mcnetwork.networkBook.markdown.block;

import de.tyro.mcnetwork.networkBook.markdown.MarkdownRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class CodeBlock extends Block {
    public final String code;
    public final String lang;

    public CodeBlock(String code, String lang) {
        this.code = code;
        this.lang = lang;
    }

    public int render(GuiGraphics gg, int x, int y, int width, int h) {
        // Draw background box
        int innerW = width - 4;
        // estimate height as number of lines * font.lineHeight
        String[] lines = this.code.split("\\r?\\n", -1);
        int boxH = lines.length * lineHeight + MarkdownRenderer.CODE_BLOCK_PADDING * 2;
        if (gg != null) {
            gg.fill(x - 4, y - 4, x + innerW + 8, y + boxH + 4, 0xFF1B1B1B); // dark bg
            int curY = y + MarkdownRenderer.CODE_BLOCK_PADDING;
            for (String line : lines) {
                String toDraw = super.trimToWidth(line, innerW);
                gg.drawString(font, Component.literal(toDraw).withStyle(ChatFormatting.GREEN), x, curY, 0xA0FFA0);
                curY += lineHeight;
            }
        }
        return boxH;
    }
}
