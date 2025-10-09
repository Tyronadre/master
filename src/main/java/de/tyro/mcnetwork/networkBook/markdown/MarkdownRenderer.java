package de.tyro.mcnetwork.networkBook.markdown;// Packagenamen an dein Projekt anpassen

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import de.tyro.mcnetwork.gui.networkBook.Scene3D.Scene3DRenderer;
import de.tyro.mcnetwork.gui.networkBook.Scene3D.SceneDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MarkdownRenderer (Parser + Renderer)
 * <p>
 * - parse(text) -> ParsedDocument (blocks cached)
 * - render(gg, parsedDoc, startX, startY, width) -> draws content, returns total drawn height
 * - estimateHeight(parsedDoc, width) -> computes height without drawing (useful for scroll clamp)
 * <p>
 * Unterstützte Blocktypen: Heading, Paragraph, CodeBlock, ListBlock, TableBlock, Hr
 * Unterstützte Inline-Formatierungen: **bold**, *italic*, `inline code`
 * <p>
 * Wichtige Abhängigkeiten:
 * - Minecraft Font: Minecraft.getInstance().font
 * - GuiGraphics für drawString
 */
public class MarkdownRenderer {

    private final Minecraft mc;
    private final Font font;

    // Basic line heights (adjustable)
    private final int lineHeight;
    private final int headingSpacing;
    private final int paragraphSpacing;
    private final int codeBlockPadding;

    public MarkdownRenderer(Minecraft mc) {
        this.mc = mc;
        this.font = mc.font;
        this.lineHeight = font.lineHeight; // approximate
        this.headingSpacing = 6;
        this.paragraphSpacing = 6;
        this.codeBlockPadding = 6;
    }

    // -------------------------
    // Public helper methods
    // -------------------------

    /**
     * Render parsed document at given position and width.
     * Returns total height drawn (useful to setup scroll limits).
     */
    public int render(GuiGraphics gg, ParsedDocument doc, int startX, int startY, int width, int clipX, int clipY, int clipW, int clipH) {
        // Enable scissor/clipping for the content area
//        enableScissor(gg, clipX, clipY, clipW, clipH);

        int y = startY;
        for (Block b : doc.blocks) {
            switch (b) {
                case AnimationBlock a -> y += renderAnimationBlock(gg, a, startX, y, width);
                case Heading h -> y += renderHeading(gg, h, startX, y, width);
                case Paragraph p -> y += renderParagraph(gg, p, startX, y, width);
                case CodeBlock c -> y += renderCodeBlock(gg, c, startX, y, width);
                case ListBlock lb -> y += renderListBlock(gg, lb, startX, y, width);
                case TableBlock tb -> y += renderTableBlock(gg, tb, startX, y, width);
                case ImageBlock ib -> y += renderImageBlock(gg, ib, startX, y, width);
                case HrBlock ignored -> y += renderHr(gg, startX, y, width);
                case null, default -> y += renderParagraph(gg, new Paragraph(""), startX, y, width);
            }
            y += b instanceof Heading ? headingSpacing : paragraphSpacing;
        }

        disableScissor();
        return y - startY;
    }

    /**
     * Estimate rendered height for the document at a specified width.
     * This performs the same layout algorithm but without drawing.
     */
    public int estimateHeight(ParsedDocument doc, int width) {
        int y = 0;
        for (Block b : doc.blocks) {
            switch (b) {
                case AnimationBlock a -> {
                    y += 600;
                }
                case Heading h -> {
                    y += estimateHeadingHeight(h, width);
                    y += headingSpacing;
                }
                case Paragraph p -> {
                    y += estimateParagraphHeight(p, width);
                    y += paragraphSpacing;
                }
                case CodeBlock c -> {
                    y += estimateCodeBlockHeight(c, width);
                    y += paragraphSpacing;
                }
                case ListBlock lb -> {
                    y += estimateListBlockHeight(lb, width);
                    y += paragraphSpacing;
                }
                case TableBlock tb -> {
                    y += estimateTableBlockHeight(tb, width);
                    y += paragraphSpacing;
                }
                case HrBlock hrBlock -> {
                    y += estimateHrHeight(width);
                    y += paragraphSpacing;
                }
                case null, default -> {
                    y += estimateParagraphHeight(new Paragraph(""), width);
                    y += paragraphSpacing;
                }
            }
        }
        return y;
    }

    // -------------------------
    // Rendering primitives
    // -------------------------

    private int renderAnimationBlock(GuiGraphics gg, AnimationBlock a, int x, int y, int width) {
        try {
            gg.fill(x,y, 500, 500, 0xFF224466);
            gg.drawString(mc.font, "Test", x, y + 500, 0xFF0000);
            a.renderer.render(gg, x, y, width, 500, Minecraft.getInstance().getFrameTimeNs());
//            return 500;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int renderHeading(GuiGraphics gg, Heading h, int x, int y, int width) {
        // Simple scaling: H1 large, H2 medium, others normal
        int textSize = Math.max(14, lineHeight + (4 - h.level) * 3); // approximate scaling
        List<StyledLine> lines = layoutInlineWords(h.text, width, ChatFormatting.BOLD, false);
        int curY = y;
        for (StyledLine sl : lines) {
            renderStyledLine(gg, sl, x, curY);
            curY += lineHeight + 2;
        }
        return curY - y;
    }

    private int estimateHeadingHeight(Heading h, int width) {
        List<StyledLine> lines = layoutInlineWords(h.text, width, ChatFormatting.BOLD, false);
        return lines.size() * (lineHeight + 2);
    }

    private int renderParagraph(GuiGraphics gg, Paragraph p, int x, int y, int width) {
        List<StyledLine> lines = layoutInlineNodes(p.inlineNodes, width);
        int curY = y;
        for (StyledLine sl : lines) {
            renderStyledLine(gg, sl, x, curY);
            curY += lineHeight;
        }
        return curY - y;
    }

    private int estimateParagraphHeight(Paragraph p, int width) {
        List<StyledLine> lines = layoutInlineNodes(p.inlineNodes, width);
        return lines.size() * lineHeight;
    }

    private int renderCodeBlock(GuiGraphics gg, CodeBlock block, int x, int y, int width) {
        // Draw background box
        int boxX = x;
        int boxY = y;
        int innerW = width - 4;
        // estimate height as number of lines * font.lineHeight
        String[] lines = block.code.split("\\r?\\n", -1);
        int boxH = lines.length * lineHeight + codeBlockPadding * 2;
        gg.fill(boxX - 4, boxY - 4, boxX + innerW + 8, boxY + boxH + 4, 0xFF1B1B1B); // dark bg
        int curY = y + codeBlockPadding;
        for (String line : lines) {
            // simple wrap for very long lines: trim to available width
            String toDraw = trimToWidth(line, innerW);
            gg.drawString(font, Component.literal(toDraw).withStyle(ChatFormatting.GREEN), boxX, curY, 0xA0FFA0);
            curY += lineHeight;
        }
        return boxH;
    }

    private int estimateCodeBlockHeight(CodeBlock block, int width) {
        String[] lines = block.code.split("\\r?\\n", -1);
        int boxH = lines.length * lineHeight + codeBlockPadding * 2;
        return boxH;
    }

    private int renderListBlock(GuiGraphics gg, ListBlock lb, int x, int y, int width) {
        int curY = y;
        int i = 1;
        for (var item : lb.items) {
            gg.drawString(font, lb.ordered ? Component.literal(i++ + ".") : Component.literal("•"), x, curY, 0xFFFFFF);
            int textX = x + 10;
            for (StyledLine sl : layoutInlineNodes(item, width - 10)) {
                renderStyledLine(gg, sl, textX, curY);
                curY += lineHeight;
            }
        }
        return curY - y;
    }

    private int estimateListBlockHeight(ListBlock lb, int width) {
        int h = 0;
        for (var item : lb.items) {
            List<StyledLine> lines = layoutInlineNodes(item, width - 10);
            h += lines.size() * lineHeight;
        }
        return h;
    }

    private static final Map<ResourceLocation, int[]> IMAGE_SIZE_CACHE = new HashMap<>();
    private int renderImageBlock(GuiGraphics gg, ImageBlock img, int x, int y, int width) {
        if (img == null || img.location == null) return 0;

        Minecraft mc = Minecraft.getInstance();
        int texW, texH;

        int[] cached = IMAGE_SIZE_CACHE.get(img.location);
        if (cached != null) {
            texW = cached[0];
            texH = cached[1];
        } else {
            texW = texH = 128;
            try {
                Resource res = mc.getResourceManager().getResource(img.location).orElse(null);
                if (res != null) {
                    try (InputStream in = res.open()) {
                        NativeImage imgData = NativeImage.read(in);
                        texW = imgData.getWidth();
                        texH = imgData.getHeight();
                        imgData.close();
                        IMAGE_SIZE_CACHE.put(img.location, new int[]{texW, texH});
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        float aspect = (float) texW / texH;
        int drawW = texW;
        int drawH = texH;

        if (drawW > width) {
            drawW = width;
            drawH = (int) (width / aspect);
        }

        int drawX = x + (width - drawW) / 2;

        gg.blit(img.location, drawX, y, 0, 0, drawW, drawH, texW, texH);

        int totalHeight = drawH;

        if (img.title != null && !img.title.isEmpty()) {
            int textWidth = mc.font.width(img.title);
            int textX = x + (width - textWidth) / 2;
            int textY = y + drawH + 4;
            gg.drawString(mc.font, img.title, textX, textY, 0xFFFFFF);
            totalHeight += mc.font.lineHeight + 6;
        }

        return totalHeight + 8;
    }


    private int renderTableBlock(GuiGraphics gg, TableBlock tb, int x, int y, int width) {
        // compute column widths based on content
        int cols = tb.rows.isEmpty() ? 0 : tb.rows.get(0).size();
        int padding = 6;
        int available = width - padding * 2;
        int[] colW = new int[cols];
        // initial measure: max content width per column
        for (int c = 0; c < cols; c++) {
            int max = 0;
            for (List<String> row : tb.rows) {
                if (c < row.size()) {
                    int w = font.width(row.get(c));
                    if (w > max) max = w;
                }
            }
            colW[c] = max + 8; // some inner padding
        }
        // If total exceeds available, scale proportionally
        int total = Arrays.stream(colW).sum();
        if (total > available) {
            float scale = (float) available / (float) total;
            for (int i = 0; i < colW.length; i++) colW[i] = Math.max(10, (int) (colW[i] * scale));
        }
        // If total less, distribute leftover
        total = Arrays.stream(colW).sum();
        if (total < available) {
            int left = available - total;
            int per = left / cols;
            for (int i = 0; i < colW.length; i++) colW[i] += per;
        }

        // Draw rows
        int curY = y;
        for (List<String> row : tb.rows) {
            int curX = x + padding;
            int rowHeight = lineHeight + 4;
            // background for header? make first row header
            if (tb.header) {
                gg.fill(x, curY, x + width, curY + rowHeight, 0xFF222233);
            }
            for (int c = 0; c < cols; c++) {
                String cell = c < row.size() ? row.get(c) : "";
                gg.drawString(font, Component.literal(cell), curX + 4, curY + 2, 0xFFFFFF);
                curX += colW[c];
            }
            curY += rowHeight;
        }
        return curY - y;
    }

    private int estimateTableBlockHeight(TableBlock tb, int width) {
        int rows = tb.rows.size();
        int rowHeight = lineHeight + 4;
        return rows * rowHeight;
    }

    private int renderHr(GuiGraphics gg, int x, int y, int width) {
        int mid = y + lineHeight / 2;
        gg.fill(x, mid, x + width, mid + 1, 0xFF444444);
        return lineHeight;
    }

    private int estimateHrHeight(int width) {
        return lineHeight;
    }

    // -------------------------
    // Inline layout & drawing
    // -------------------------

    // StyledLine = one visual line consisting of StyledWords
    private static class StyledLine {
        List<StyledWord> words = new ArrayList<>();
    }

    private static class StyledWord {
        Component comp;

        public StyledWord(String text, boolean bold, boolean italic, boolean code) {
            var comp = Component.literal(text);
            if (code) comp.withStyle(ChatFormatting.DARK_GRAY);
            if (bold) comp.withStyle(ChatFormatting.BOLD);
            if (italic) comp.withStyle(ChatFormatting.ITALIC);
            this.comp = comp;
        }

        public Component getComponent() {
            return comp;
        }
    }

    // Layout inlineNodes into lines (respecting width); returns StyledLines ready for drawing
    private List<StyledLine> layoutInlineNodes(List<InlineNode> nodes, int width) {
        // convert nodes into words with style
        List<StyledWord> words = inlineNodesToWords(nodes);
        return layoutWords(words, width);
    }

    // Convenience: layout plain text into words with single style
    private List<StyledLine> layoutInlineWords(String text, int width, ChatFormatting forceFormat, boolean code) {
        List<StyledWord> words = new ArrayList<>();
        String[] parts = text.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            String w = parts[i];
            boolean bold = forceFormat == ChatFormatting.BOLD;
            boolean italic = forceFormat == ChatFormatting.ITALIC;
            words.add(new StyledWord(w, bold, italic, code));
        }
        return layoutWords(words, width);
    }

    // Convert inline nodes to words with style preservation
    private List<StyledWord> inlineNodesToWords(List<InlineNode> nodes) {
        List<StyledWord> out = new ArrayList<>();
        for (InlineNode n : nodes) {
            if (n instanceof TextNode) {
                String[] parts = ((TextNode) n).text.split("\\s+");
                for (String p : parts) {
                    if (p.length() > 0) out.add(new StyledWord(p, false, false, false));
                }
            } else if (n instanceof BoldNode) {
                String plain = ((BoldNode) n).text;
                String[] parts = plain.split("\\s+");
                for (String p : parts) {
                    if (p.length() > 0) out.add(new StyledWord(p, true, false, false));
                }
            } else if (n instanceof ItalicNode) {
                String plain = ((ItalicNode) n).text;
                String[] parts = plain.split("\\s+");
                for (String p : parts) {
                    if (p.length() > 0) out.add(new StyledWord(p, false, true, false));
                }
            } else if (n instanceof InlineCodeNode) {
                String plain = ((InlineCodeNode) n).text;
                String[] parts = plain.split("\\s+");
                for (String p : parts) {
                    if (p.length() > 0) out.add(new StyledWord(p, false, false, true));
                }
            } else {
                // fallback
                String s = n.toString();
                if (s != null) {
                    String[] parts = s.split("\\s+");
                    for (String p : parts) if (p.length() > 0) out.add(new StyledWord(p, false, false, false));
                }
            }
        }
        return out;
    }

    // Word-wrapping: pack words into StyledLines so that width constraint is satisfied
    private List<StyledLine> layoutWords(List<StyledWord> words, int width) {
        List<StyledLine> lines = new ArrayList<>();
        if (words.isEmpty()) {
            lines.add(new StyledLine());
            return lines;
        }
        int spaceWidth = font.width(" ");
        StyledLine cur = new StyledLine();
        int curW = 0;
        boolean firstWord = true;
        for (StyledWord w : words) {
            int wW = font.width(w.getComponent());
            int add = (firstWord ? 0 : spaceWidth) + wW;
            if (curW + add > width && !firstWord) {
                // push current line
                lines.add(cur);
                cur = new StyledLine();
                curW = 0;
                firstWord = true;
                // put word to new line
                cur.words.add(w);
                curW += wW;
                firstWord = false;
            } else {
                if (!firstWord) {
                    // add space as separate pseudo-word
                    cur.words.add(new StyledWord(" ", false, false, false));
                    curW += spaceWidth;
                }
                cur.words.add(w);
                curW += wW;
                firstWord = false;
            }
        }
        // push last
        lines.add(cur);
        return lines;
    }

    // Render a StyledLine: iterate its words and draw each with style
    private void renderStyledLine(GuiGraphics gg, StyledLine sl, int x, int y) {
        int curX = x;
        for (StyledWord w : sl.words) {
            var comp = w.getComponent();
            int color = comp.getStyle().getColor() == null ? 0xFFFFFF : comp.getStyle().getColor().getValue();
            gg.drawString(font, comp, curX, y, color);
            curX += font.width(comp);
        }
    }

    // Simple helper to trim long lines for code blocks
    private String trimToWidth(String s, int w) {
        if (font.width(s) <= w) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(s.charAt(i));
            if (font.width(sb.toString() + "...") > w) {
                return sb.substring(0, Math.max(0, sb.length() - 1)) + "...";
            }
        }
        return s;
    }

    // -------------------------
    // Document / Block / Inline model
    // -------------------------

    public static class ParsedDocument {
        public final List<Block> blocks = new ArrayList<>();
    }

    static abstract class Block {
    }

    static class Heading extends Block {
        public final int level;
        public final String text;

        public Heading(int level, String text) {
            this.level = level;
            this.text = text;
        }
    }

    static class Paragraph extends Block {
        public final List<InlineNode> inlineNodes;

        public Paragraph(List<InlineNode> nodes) {
            this.inlineNodes = nodes;
        }

        public Paragraph(String plain) {
            this.inlineNodes = Collections.singletonList(new TextNode(plain));
        }
    }

    static class ImageBlock extends Block {
        public final ResourceLocation location;
        public final String title;

        public ImageBlock(ResourceLocation location, String title) {
            this.location = location;
            this.title = title;
        }
    }

    static class CodeBlock extends Block {
        public final String code;
        public final String lang;

        public CodeBlock(String code, String lang) {
            this.code = code;
            this.lang = lang;
        }
    }

    static class ListBlock extends Block {
        public final List<List<InlineNode>> items;
        public final boolean ordered;

        public ListBlock(List<List<InlineNode>> items, boolean ordered) {
            this.items = items;
            this.ordered = ordered;
        }
    }

    static class TableBlock extends Block {
        public final List<List<String>> rows;
        public final boolean header;

        public TableBlock(List<List<String>> rows, boolean header) {
            this.rows = rows;
            this.header = header;
        }
    }

    static class HrBlock extends Block {
    }

    static class AnimationBlock extends Block {
        public final Scene3DRenderer renderer;

        public AnimationBlock(ResourceLocation location) {
            this.renderer = new Scene3DRenderer(SceneDefinition.load(location));
        }
    }

    // Inline nodes
    static abstract class InlineNode {
        public String text;

        public String getText() {
            return text;
        }
    }

    static class TextNode extends InlineNode {
        public TextNode(String text) {
            this.text = text;
        }
    }

    static class BoldNode extends InlineNode {
        public BoldNode(String t) {
            this.text = t;
        }
    }

    static class ItalicNode extends InlineNode {
        public ItalicNode(String t) {
            this.text = t;
        }
    }

    static class InlineCodeNode extends InlineNode {
        public InlineCodeNode(String t) {
            this.text = t;
        }
    }

    // -------------------------
    // Scissor / Clipping helpers
    // -------------------------

    private void enableScissor(GuiGraphics gg, int x, int y, int w, int h) {
        // convert GUI coordinates to framebuffer coordinates
        // We need to account for GUI scale
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        // Note: Gui origin (0,0) top-left — RenderSystem.scissor uses framebuffer coords with origin bottom-left
        // compute scale factors
        double scaleX = (double) mc.getWindow().getWidth() / screenWidth;
        double scaleY = (double) mc.getWindow().getHeight() / screenHeight;
        int sx = (int) Math.round(x * scaleX);
        int sy = (int) Math.round((screenHeight - (y + h)) * scaleY);
        int sw = (int) Math.round(w * scaleX);
        int sh = (int) Math.round(h * scaleY);
        RenderSystem.enableScissor(sx, sy, sw, sh);
    }

    private void disableScissor() {
        RenderSystem.disableScissor();
    }
}
