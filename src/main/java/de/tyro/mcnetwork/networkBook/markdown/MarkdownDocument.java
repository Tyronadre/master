package de.tyro.mcnetwork.networkBook.markdown;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class MarkdownDocument {
    private final List<Node> nodes = new ArrayList<>();

    public int estimateHeight(MarkdownRenderer renderer, int width) {
        return nodes.stream().map(node -> node.estimateHeight(renderer, width)).reduce(0, Integer::sum);
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public List<Node> getNodes() {
        return nodes;
    }

    abstract public static class Node {
        abstract int estimateHeight(MarkdownRenderer renderer, int width);
    }

    public static class HeaderNode extends Node {
        private final TextNode text;

        public HeaderNode(String text, int level) {

            this.text = TextNode.builder(text).size(switch (level) {
                case 1 -> 24;
                case 2 -> 20;
                case 3 -> 18;
                case 4 -> 16;
                case 5 -> 14;
                default -> 12;
            }).bold().build();
        }

        @Override
        int estimateHeight(MarkdownRenderer renderer, int width) {
            return text.estimateHeight(renderer, width);
        }
    }

    static class ParagraphNode extends Node {
        private final List<TextNode> text;

        public ParagraphNode(List<TextNode> text) {
            this.text = text;
        }

        @Override
        int estimateHeight(MarkdownRenderer renderer, int width) {
            renderer.layoutText(text, width);
            return text.stream().map(t -> t.estimateHeight(renderer, width)).reduce(0, Integer::sum);
        }
    }

    static class ImageNode extends Node {
        public final String location;
        public final String title;

        public ImageNode(String location, String title) {
            this.location = location;
            this.title = title;
        }

        @Override
        int estimateHeight(MarkdownRenderer renderer, int width) {
            return 100; // TODO: placeholder
        }
    }

    static class CodeNode extends Node {
        public final String code;
        public final String lang;

        public CodeNode(String code, String lang) {
            this.code = code;
            this.lang = lang;
        }

        @Override
        int estimateHeight(MarkdownRenderer renderer, int width) {
            var lines = code.split("\n", -1);
            return lines.length * renderer.getLineHeight() + renderer.getCodeBlockPadding() * 2; // TODO: placeholder
        }
    }

    static class ListNode extends Node {
        public final List<List<TextNode>> items;
        public final boolean ordered;

        public ListNode(boolean ordered) {
            this.ordered = ordered;
            this.items = new ArrayList<>();
        }

        public void addItem(List<TextNode> item) {
            items.add(item);
        }

        @Override
        int estimateHeight(MarkdownRenderer renderer, int width) {
            return items.stream().map(i -> i.stream().map(t -> t.estimateHeight(renderer, width - 10)).reduce(0, Integer::sum)).reduce(0, Integer::sum);
        }
    }

    static class TableNode extends Node {
        public final List<List<List<TextNode>>> rows;
        public final boolean header;

        public TableNode(List<List<List<TextNode>>> rows, boolean header) {
            this.rows = rows;
            this.header = header;
        }

        @Override
        int estimateHeight(MarkdownRenderer renderer, int width) {
            return rows.size() * renderer.getLineHeight() + 4;
        }
    }

    static class HrNode extends Node {
        @Override
        int estimateHeight(MarkdownRenderer renderer, int width) {
            return renderer.getLineHeight();
        }
    }

    static class TextNode extends Node {
        public final String text;
        public final Boolean bold;
        public final Boolean italic;
        public final Boolean code;
        public final Integer color;
        public final Integer size;

        public TextNode(String text, Boolean bold, Boolean italic, Boolean code, Integer color, Integer size) {
            this.text = text;
            this.bold = bold;
            this.italic = italic;
            this.code = code;
            this.color = color;
            this.size = size;
        }

        public static TextNodeBuilder builder(String text) {
            return new TextNodeBuilder(text);
        }

        @Override
        int estimateHeight(MarkdownRenderer renderer, int width) {
            return renderer.getLineHeight();
        }

        static class TextNodeBuilder {
            private final String text;
            private boolean bold;
            private boolean italic;
            private boolean code;
            private int color;
            private int size;

            public TextNodeBuilder(String text) {
                this.text = text;
            }

            public TextNodeBuilder bold() {
                this.bold = true;
                return this;
            }

            public TextNodeBuilder italic() {
                this.italic = true;
                return this;
            }

            public TextNodeBuilder code() {
                this.code = true;
                return this;
            }

            public TextNodeBuilder color(int color) {
                this.color = color;
                return this;
            }

            public TextNodeBuilder size(int size) {
                this.size = size;
                return this;
            }

            public TextNode build() {
                return new TextNode(text, bold, italic, code, color, size);
            }
        }
    }


}