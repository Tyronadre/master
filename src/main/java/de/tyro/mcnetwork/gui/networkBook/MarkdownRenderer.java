package de.tyro.mcnetwork.gui.networkBook;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight Markdown-like renderer:
 * - lines starting with "# " -> H1
 * - "## " -> H2
 * - code blocks surrounded by ``` -> monospaced block
 * - paragraphs
 *
 * This is intentionally minimal — extend with proper Markdown parser if desired.
 */
public class MarkdownRenderer {

    private final Minecraft mc;

    public MarkdownRenderer(Minecraft mc) {
        this.mc = mc;
    }

    public void render(GuiGraphics gg, String markdown, int startX, int startY) {
        if (markdown == null || markdown.isEmpty()) return;
        List<String> lines = parseToLines(markdown);
        int y = startY;
        for (String line : lines) {
            // heading detection
            if (line.startsWith("# ")) {
                gg.drawString(mc.font, line.substring(2), startX, y, 0xFFFFFF);
                y += 14;
            } else if (line.startsWith("## ")) {
                gg.drawString(mc.font, line.substring(3), startX, y, 0xCCCCCC);
                y += 12;
            } else if (line.startsWith("```")) {
                // code block: subsequent lines until closing ```
                // For simplicity: treat as monospaced block placeholder
                gg.fill(startX - 6, y - 2, startX + 300, y + 60, 0xFF1E1E1E);
                gg.drawString(mc.font, "<code block>", startX, y, 0x88FF88);
                y += 64;
            } else {
                // normal paragraph (wrap naively)
                gg.drawString(mc.font, line, startX, y, 0xBBBBBB);
                y += 10;
            }
        }
    }

    private List<String> parseToLines(String markdown) {
        // naive split by newline
        String[] raw = markdown.split("\\r?\\n");
        List<String> out = new ArrayList<>();
        for (String s : raw) {
            out.add(s.trim());
        }
        return out;
    }

    public int estimateHeight(String markdown, int width) {
        // naive: count lines * ~12 px
        if (markdown == null || markdown.isEmpty()) return 0;
        String[] raw = markdown.split("\\r?\\n");
        return raw.length * 12 + 20;
    }
}
