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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MarkdownRenderer (Parser + Renderer)
 * <p.yaml>
 * - parse(text) -> ParsedDocument (blocks cached)
 * - render(gg, parsedDoc, startX, startY, width) -> draws content, returns total drawn height
 * - estimateHeight(parsedDoc, width) -> computes height without drawing (useful for scroll clamp)
 * <p.yaml>
 * Unterstützte Blocktypen: Heading, Paragraph, CodeBlock, ListBlock, TableBlock, Hr
 * Unterstützte Inline-Formatierungen: **bold**, *italic*, `inline code`
 * <p.yaml>
 * Wichtige Abhängigkeiten:
 * - Minecraft Font: Minecraft.getInstance().font
 * - GuiGraphics für drawString
 */
public class MarkdownRenderer {

    private final Minecraft mc;
    private final Font font;
    private final MarkdownDocument document = new MarkdownDocument();

    // Basic line heights (adjustable)
    private final int lineHeight;
    private final int headingSpacing;
    private final int paragraphSpacing;
    private final int codeBlockPadding;

    public MarkdownRenderer() {
        this.mc = Minecraft.getInstance();
        this.font = mc.font;
        this.lineHeight = font.lineHeight; // approximate
        this.headingSpacing = 6;
        this.paragraphSpacing = 6;
        this.codeBlockPadding = 6;
    }

    /**
     * Render parsed document at given position and width.
     * Returns total height drawn (useful to setup scroll limits).
     */
    public int render(GuiGraphics gg, MarkdownDocument doc, int startX, int startY, int width, int clipX, int clipY, int clipW, int clipH) {
        enableScissor(gg, clipX, clipY, clipW, clipH);

        int y = startY;
        for (Block b : doc.blocks) {
            y += b.render(gg, startX, y, width);
            y += b instanceof HeaderBlock ? headingSpacing : paragraphSpacing;
        }
        //gg.fill(0, 0, 1000, 1000,0x2FF24466 );
        disableScissor();
        return y - startY;
    }

    public MarkdownDocument getDocument() {
        return this.document;
    }

    private List<StyledLine> layoutInlineNodes(List<InlineNode> nodes, int width) {
        List<StyledWord> words = inlineNodesToWords(nodes);
        return layoutWords(words, width);
    }

    private List<StyledLine> layoutInlineWords(String text, int width, ChatFormatting forceFormat, boolean code) {
        List<StyledWord> words = new ArrayList<>();
        String[] parts = text.split("\\s+");
        for (String w : parts) {
            boolean bold = forceFormat == ChatFormatting.BOLD;
            boolean italic = forceFormat == ChatFormatting.ITALIC;
            words.add(new StyledWord(w, bold, italic, code));
        }
        return layoutWords(words, width);
    }

    private List<StyledWord> inlineNodesToWords(List<InlineNode> nodes) {
        List<StyledWord> out = new ArrayList<>();
        for (InlineNode n : nodes) {
            switch (n) {
                case TextNode ignored -> {
                    String[] parts = n.text.split("\\s+");
                    for (String p : parts) {
                        if (!p.isEmpty()) out.add(new StyledWord(p, false, false, false));
                    }
                }
                case BoldNode ignored -> {
                    String plain = n.text;
                    String[] parts = plain.split("\\s+");
                    for (String p : parts) {
                        if (!p.isEmpty()) out.add(new StyledWord(p, true, false, false));
                    }
                }
                case ItalicNode ignored -> {
                    String plain = n.text;
                    String[] parts = plain.split("\\s+");
                    for (String p : parts) {
                        if (!p.isEmpty()) out.add(new StyledWord(p, false, true, false));
                    }
                }
                case InlineCodeNode ignored -> {
                    String plain = n.text;
                    String[] parts = plain.split("\\s+");
                    for (String p : parts) {
                        if (!p.isEmpty()) out.add(new StyledWord(p, false, false, true));
                    }
                }
                default -> {
                    // fallback
                    String s = n.toString();
                    if (s != null) {
                        String[] parts = s.split("\\s+");
                        for (String p : parts) if (!p.isEmpty()) out.add(new StyledWord(p, false, false, false));
                    }
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
                lines.add(cur);
                cur = new StyledLine();
                curW = 0;
            } else {
                if (!firstWord) {
                    cur.words.add(new StyledWord(" ", false, false, false));
                    curW += spaceWidth;
                }
            }
            cur.words.add(w);
            curW += wW;
            firstWord = false;
        }
        lines.add(cur);
        return lines;
    }

    private void renderStyledLine(GuiGraphics gg, StyledLine sl, int x, int y) {
        int curX = x;
        for (StyledWord w : sl.words) {
            var comp = w.getComponent();
            int color = comp.getStyle().getColor() == null ? 0xFFFFFF : comp.getStyle().getColor().getValue();
            gg.drawString(font, comp, curX, y, color);
            curX += font.width(comp);
        }
    }

    private String trimToWidth(String s, int w) {
        if (font.width(s) <= w) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(s.charAt(i));
            if (font.width(sb + "...") > w) {
                return sb.substring(0, Math.max(0, sb.length() - 1)) + "...";
            }
        }
        return s;
    }

    private void enableScissor(GuiGraphics gg, int x, int y, int w, int h) {
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
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

    abstract static class Block {
        abstract int render(GuiGraphics g, int x, int y, int w);
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

    public class MarkdownDocument {
        final List<Block> blocks = new ArrayList<>();

        public void addImageBlock(ResourceLocation resourceLocation, String title) {
            blocks.add(new ImageBlock(resourceLocation, title));
        }

        public void addAnimationBlock(ResourceLocation resourceLocation) {
            blocks.add(new AnimationBlock(resourceLocation));
        }

        public void addCodeBlock(String string, String codeLang) {
            blocks.add(new CodeBlock(string, codeLang));
        }

        public void addHrBlock() {
            blocks.add(new HrBlock());
        }

        public void addHeaderBlock(int level, String text) {
            blocks.add(new HeaderBlock(level, text));
        }

        public void addParagraphBlock(List<InlineNode> inlineNodes) {
            blocks.add(new ParagraphBlock(inlineNodes));
        }

        public void addTableBlock(List<List<String>> rows, boolean header) {
            blocks.add(new TableBlock(rows, header));
        }

        public void addListBlock(List<List<InlineNode>> list, boolean ordered) {
            blocks.add(new ListBlock(list, ordered));
        }
    }

    class HeaderBlock extends Block {
        public final int level;
        public final String text;

        public HeaderBlock(int level, String text) {
            this.level = level;
            this.text = text;
        }

        @Override
        protected int render(GuiGraphics gg, int x, int y, int width) {
            // Simple scaling: H1 large, H2 medium, others normal
            int textSize = Math.max(14, lineHeight + (4 - level) * 3); // approximate scaling
            List<StyledLine> lines = layoutInlineWords(text, width, ChatFormatting.BOLD, false);
            int curY = y;
            for (StyledLine sl : lines) {
                renderStyledLine(gg, sl, x, curY);
                curY += lineHeight + 2;
            }
            return curY - y;
        }
    }

    class ParagraphBlock extends Block {
        public final List<InlineNode> inlineNodes;

        public ParagraphBlock(List<InlineNode> nodes) {
            this.inlineNodes = nodes;
        }

        @Override
        public int render(GuiGraphics gg, int x, int y, int width) {
            List<StyledLine> lines = layoutInlineNodes(inlineNodes, width);
            int curY = y;
            for (StyledLine sl : lines) {
                renderStyledLine(gg, sl, x, curY);
                curY += lineHeight;
            }
            return curY - y;
        }
    }

    class ImageBlock extends Block {
        public final ResourceLocation location;
        public final String title;
        public final int height;
        public final int width;

        public ImageBlock(ResourceLocation location, String title) {
            this.location = location;
            this.title = title;

            int tHeight = 0;
            int tWidth = 0;

            Minecraft mc = Minecraft.getInstance();
            try {
                Resource res = mc.getResourceManager().getResource(location).orElse(null);
                if (res != null) {
                    InputStream in = res.open();
                    NativeImage imgData = NativeImage.read(in);
                    tWidth = imgData.getWidth() / 2;
                    tHeight = imgData.getHeight() / 2;
                    imgData.close();
                    in.close();
                }
            } catch (Exception e) {
                System.err.println("Failed to load image " + location);
            }

            height = tHeight;
            width = tWidth;
        }

        @Override
        protected int render(GuiGraphics gg, int x, int y, int width) {
            if (location == null) return 0;


            int drawW = this.width;
            int drawH = this.height;
            float aspect = (float) drawW / (float) drawH;


            if (drawW > width) {
                drawW = width;
                drawH = (int) (width / aspect);
            }

            int drawX = x + (width - drawW) / 2;

            gg.blit(location, drawX, y, 0, 0, drawW, drawH, this.width, this.height);

            int totalHeight = drawH;

            if (title != null && !title.isEmpty()) {
                int textWidth = mc.font.width(title);
                int textX = x + (width - textWidth) / 2;
                int textY = y + drawH + 4;
                gg.drawString(mc.font, title, textX, textY, 0xFFFFFF);
                totalHeight += mc.font.lineHeight + 6;
            }

            return totalHeight + 8;
        }
    }

    class CodeBlock extends Block {
        public final String code;
        public final String lang;

        public CodeBlock(String code, String lang) {
            this.code = code;
            this.lang = lang;
        }

        public int render(GuiGraphics gg, int x, int y, int width) {
            // Draw background box
            int innerW = width - 4;
            // estimate height as number of lines * font.lineHeight
            String[] lines = this.code.split("\\r?\\n", -1);
            int boxH = lines.length * lineHeight + codeBlockPadding * 2;
            gg.fill(x - 4, y - 4, x + innerW + 8, y + boxH + 4, 0xFF1B1B1B); // dark bg
            int curY = y + codeBlockPadding;
            for (String line : lines) {
                String toDraw = trimToWidth(line, innerW);
                gg.drawString(font, Component.literal(toDraw).withStyle(ChatFormatting.GREEN), x, curY, 0xA0FFA0);
                curY += lineHeight;
            }
            return boxH;
        }
    }

    class ListBlock extends Block {
        public final List<List<InlineNode>> items;
        public final boolean ordered;

        public ListBlock(List<List<InlineNode>> items, boolean ordered) {
            this.items = items;
            this.ordered = ordered;
        }

        @Override
        protected int render(GuiGraphics gg, int x, int y, int width) {
            int curY = y;
            int i = 1;
            var spaces = (int) Math.floor(Math.log10(items.size())) * 5;
            for (var item : items) {
                gg.drawString(font, ordered ? Component.literal(i++ + ".") : Component.literal("•"), x, curY, 0xFFFFFF);
                for (StyledLine sl : layoutInlineNodes(item, width - 10)) {
                    renderStyledLine(gg, sl, x + 10 + spaces, curY);
                    curY += lineHeight;
                }
            }
            return curY - y;
        }
    }

    class TableBlock extends Block {
        public final List<List<String>> rows;
        public final boolean header;

        public TableBlock(List<List<String>> rows, boolean header) {
            this.rows = rows;
            this.header = header;
        }

        //TODO Should prob overflow instead of mashing into itself
        @Override
        protected int render(GuiGraphics gg, int x, int y, int width) {
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
                if (header) {
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

    }

    class HrBlock extends Block {
        @Override
        protected int render(GuiGraphics gg, int x, int y, int width) {
            int mid = y + lineHeight / 2;
            gg.fill(x, mid, x + width, mid + 1, 0xFF444444);
            return lineHeight;
        }
    }

    //TODO
    class AnimationBlock extends Block {
        public final Scene3DRenderer renderer;

        public AnimationBlock(ResourceLocation location) {
            this.renderer = new Scene3DRenderer(SceneDefinition.load(location));
        }


        @Override
        protected int render(GuiGraphics gg, int x, int y, int width) {
            try {
                gg.fill(x, y, 500, 500, 0xFF224466);
                gg.drawString(mc.font, "Test", x, y + 500, 0xFF0000);
                this.renderer.render(gg, x, y, width, 500, Minecraft.getInstance().getFrameTimeNs());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }
    }
}
