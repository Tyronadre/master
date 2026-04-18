package de.tyro.mcnetwork.networkBook.markdown.block;

import de.tyro.mcnetwork.networkBook.markdown.MarkdownRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class CodeBlock extends Block {
    public final String code;
    public final String lang;
    private final String[] lines;
    private int lastWidth = -1;
    private Component[] cachedComponents;

    public CodeBlock(String code, String lang) {
        this.code = code;
        this.lang = lang;
        this.lines = this.code.split("\\r?\\n", -1);
    }

    public int render(GuiGraphics gg, int x, int y, int width, int h) {
        // Draw background box
        int innerW = width - 4;
        // estimate height as number of lines * font.lineHeight
        int boxH = lines.length * lineHeight + MarkdownRenderer.CODE_BLOCK_PADDING * 2;
        if (gg != null) {
            if (lastWidth != innerW) {
                cachedComponents = new Component[lines.length];
                for (int i = 0; i < lines.length; i++) {
                    cachedComponents[i] = Component.literal(super.trimToWidth(lines[i], innerW)).withStyle(ChatFormatting.GREEN);
                }
                lastWidth = innerW;
            }
            gg.fill(x - 4, y - 4, x + innerW + 8, y + boxH + 4, 0xFF1B1B1B); // dark bg
            int curY = y + MarkdownRenderer.CODE_BLOCK_PADDING;
            for (Component toDraw : cachedComponents) {
                gg.drawString(font, toDraw, x, curY, 0xA0FFA0);
                curY += lineHeight;
            }
        }
        return boxH;
    }
}
