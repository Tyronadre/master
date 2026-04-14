package de.tyro.mcnetwork.networkBook.markdown.block;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public abstract class Block {
    protected static final Minecraft mc = Minecraft.getInstance();
    protected static final Font font = mc.font;
    protected static final int lineHeight = mc.font.lineHeight;

    public abstract int render(GuiGraphics g, int x, int y, int w, int h);

    protected String trimToWidth(String s, int w) {
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

    protected static class StyledLine {
        List<StyledWord> words = new ArrayList<>();
    }

    // Inline nodes
    public static abstract class InlineNode {
        public String text;

        public String getText() {
            return text;
        }
    }

    public static class TextNode extends InlineNode {
        public TextNode(String text) {
            this.text = text;
        }
    }

    public static class BoldNode extends InlineNode {
        public BoldNode(String t) {
            this.text = t;
        }
    }

    public static class ItalicNode extends InlineNode {
        public ItalicNode(String t) {
            this.text = t;
        }
    }

    public static class InlineCodeNode extends InlineNode {
        public InlineCodeNode(String t) {
            this.text = t;
        }
    }

    protected List<StyledLine> layoutInlineNodes(List<InlineNode> nodes, int width) {
        List<StyledWord> words = inlineNodesToWords(nodes);
        return layoutWords(words, width);
    }

    protected List<StyledLine> layoutInlineWords(String text, int width, ChatFormatting forceFormat, boolean code) {
        List<StyledWord> words = new ArrayList<>();
        String[] parts = text.split("\\s+");
        for (String w : parts) {
            boolean bold = forceFormat == ChatFormatting.BOLD;
            boolean italic = forceFormat == ChatFormatting.ITALIC;
            words.add(new StyledWord(w, bold, italic, code));
        }
        return layoutWords(words, width);
    }

    protected List<StyledWord> inlineNodesToWords(List<InlineNode> nodes) {
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
    protected List<StyledLine> layoutWords(List<StyledWord> words, int width) {
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

    protected void renderStyledLine(GuiGraphics gg, StyledLine sl, int x, int y) {
        if (gg == null) return;
        int curX = x;
        for (StyledWord w : sl.words) {
            var comp = w.getComponent();
            int color = comp.getStyle().getColor() == null ? 0xFFFFFF : comp.getStyle().getColor().getValue();
            gg.drawString(font, comp, curX, y, color);
            curX += font.width(comp);
        }
    }

    protected static class StyledWord {
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
}
