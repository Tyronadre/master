package de.tyro.mcnetwork.networkBook.markdown;

import java.util.ArrayList;
import java.util.List;

public class MarkdownDocument {
    private final List<Node> nodes = new ArrayList<>();

    public void addNode(Node node) {
        nodes.add(node);
    }

    public List<Node> getNodes() {
        return nodes;
    }

    abstract public static class Node {

    }

    public static class HeaderNode extends Node {
        private final String text;
        private final int level;

        public HeaderNode(String text, int level) {
            this.text = text;
            this.level = level;
        }
    }

    public static class ParagraphNode extends Node {
        private final String text;

        public ParagraphNode(String text) {
            this.text = text;
        }
    }

    static class ImageNode extends Node {
        public final String location;
        public final String title;

        public ImageNode(String location, String title) {
            this.location = location;
            this.title = title;
        }
    }

    static class CodeNode extends Node {
        public final String code;
        public final String lang;

        public CodeNode(String code, String lang) {
            this.code = code;
            this.lang = lang;
        }
    }

    static class ListNode extends Node {
        public final List<List<MarkdownRenderer.InlineNode>> items;
        public final boolean ordered;

        public ListNode(List<List<MarkdownRenderer.InlineNode>> items, boolean ordered) {
            this.items = items;
            this.ordered = ordered;
        }
    }

    static class TableNode extends Node {
        public final List<List<String>> rows;
        public final boolean header;

        public TableNode(List<List<String>> rows, boolean header) {
            this.rows = rows;
            this.header = header;
        }
    }

    static class HrNode extends Node {
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