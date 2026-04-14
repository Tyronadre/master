package de.tyro.mcnetwork.networkBook.markdown.block;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

public class TableBlock extends Block {
    public final List<List<String>> rows;
    public final boolean header;

    public TableBlock(List<List<String>> rows, boolean header) {
        this.rows = rows;
        this.header = header;
    }

    //TODO Should prob overflow instead of mashing into itself
    @Override
    public int render(GuiGraphics gg, int x, int y, int width, int h) {
        int cols = rows.isEmpty() ? 0 : rows.getFirst().size();
        int padding = 6;
        int available = width - padding * 2;
        int[] colW = new int[cols];
        for (int c = 0; c < cols; c++) {
            int max = 0;
            for (List<String> row : rows) {
                if (c < row.size()) {
                    int w = font.width(row.get(c));
                    if (w > max) max = w;
                }
            }
            colW[c] = max + 8;
        }
        int total = Arrays.stream(colW).sum();
        if (total > available) {
            float scale = (float) available / (float) total;
            for (int i = 0; i < colW.length; i++) colW[i] = Math.max(10, (int) (colW[i] * scale));
        }
        total = Arrays.stream(colW).sum();
        if (total < available) {
            int left = available - total;
            int per = left / cols;
            for (int i = 0; i < colW.length; i++) colW[i] += per;
        }

        // rows
        int curY = y;
        for (List<String> row : rows) {
            int curX = x + padding;
            int rowHeight = lineHeight + 4;
            if (header && gg != null) {
                gg.fill(x, curY, x + width, curY + rowHeight, 0xFF222233);
            }
            for (int c = 0; c < cols; c++) {
                String cell = c < row.size() ? row.get(c) : "";
                if (gg != null) {
                    gg.drawString(font, Component.literal(cell), curX + 4, curY + 2, 0xFFFFFF);
                }
                curX += colW[c];
            }
            curY += rowHeight;
        }
        return curY - y;
    }

}
