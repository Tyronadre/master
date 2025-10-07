package de.tyro.mcnetwork.networkBook.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownParser {

    private static final Pattern headingPattern = Pattern.compile("^(#{1,6})\\s*(.*)$");
    private static final Pattern listItemPattern = Pattern.compile("^\\s*[-*]\\s+(.*)$");
    private static final Pattern hrPattern = Pattern.compile("^(-{3,}|\\*{3,})\\s*$");
    private static final Pattern tableLinePattern = Pattern.compile("^\\s*\\|.*\\|\\s*$");
    private static final Pattern fencedCodePattern = Pattern.compile("^```(?:\\s*(\\w+))?\\s*$");
    private static final Pattern orderedListItemPattern = Pattern.compile("\\d+\\.\\s+(.*)$");
    private static final Pattern imagePattern = Pattern.compile("!\\[.*?]\\(([\\w.]*)\\s*(\".*\")?.*\\)");

    public static MarkdownDocument parse(String markdown) {

        MarkdownDocument document = new MarkdownDocument();
        String[] lines = markdown.split("\\r?\\n", -1);
        int i = 0;
        boolean inCode = false;
        StringBuilder codeBuf = new StringBuilder();
        String codeLang = null;

        while (i < lines.length) {
            String line = lines[i];

            // Empty Line
            if (line.trim().isEmpty()) {
                i++;
                continue;
            }

            // Image
            Matcher img = imagePattern.matcher(line);
            if (img.find()) {
                document.addNode(new MarkdownDocument.ImageNode(img.group(1), img.group(2)));
                i++;
                continue;
            }

            // Ordered List
            Matcher oList = orderedListItemPattern.matcher(line);
            if (oList.matches()) {
                var listNode = new MarkdownDocument.ListNode(true);
                i = collectListItems(lines, i, oList, listNode);
                continue;
            }

            // Unordered List
            Matcher li = listItemPattern.matcher(line);
            if (li.matches()) {
                var listNode = new MarkdownDocument.ListNode(false);
                i = collectListItems(lines, i, oList, listNode);
            }

            // CodeBlock
            Matcher fence = fencedCodePattern.matcher(line);
            if (fence.matches()) {
                if (!inCode) {
                    inCode = true;
                    codeBuf.setLength(0);
                    codeLang = fence.group(1);
                } else {
                    // end fenced code
                    document.addNode(new MarkdownDocument.CodeNode(codeBuf.toString(), codeLang));
                    inCode = false;
                    codeLang = null;
                }
                i++;
                continue;
            }
            if (inCode) {
                codeBuf.append(line).append("\n");
                i++;
                continue;
            }

            // Horizontal rule
            if (hrPattern.matcher(line).matches()) {
                document.addNode(new MarkdownDocument.HrNode());
                i++;
                continue;
            }

            // Header
            Matcher hm = headingPattern.matcher(line);
            if (hm.matches()) {
                document.addNode(new MarkdownDocument.HeaderNode(hm.group(2).trim(), hm.group(1).length()));
                i++;
                continue;
            }

            // Table
            if (tableLinePattern.matcher(line).matches()) {
                List<String> tableBuffer = new ArrayList<>();
                tableBuffer.add(line.trim());
                i++;
                while (i < lines.length && tableLinePattern.matcher(lines[i]).matches()) {
                    tableBuffer.add(lines[i].trim());
                    i++;
                }
                parseTable(tableBuffer, document);
                continue;
            }

            // Paragraph: collect until blank or other block marker
            StringBuilder para = new StringBuilder();
            para.append(line);
            i++;
            while (i < lines.length && !lines[i].trim().isEmpty() && !headingPattern.matcher(lines[i]).matches() && !listItemPattern.matcher(lines[i]).matches() && !tableLinePattern.matcher(lines[i]).matches() && !hrPattern.matcher(lines[i]).matches() && !fencedCodePattern.matcher(lines[i]).matches()) {
                para.append("\n").append(lines[i]);
                i++;
            }
            document.addNode(new MarkdownDocument.ParagraphNode(parseInline(para.toString())));
        }

        return document;
    }

    private static int collectListItems(String[] lines, int i, Matcher oList, MarkdownDocument.ListNode listNode) {
        listNode.addItem(parseInline(oList.group(1)));
        i++;
        while (i < lines.length) {
            Matcher oList2 = orderedListItemPattern.matcher(lines[i]);
            if (oList2.matches()) {
                listNode.addItem(parseInline(oList2.group(1)));
                i++;
            } else break;
        }
        return i;
    }

    private static void parseTable(List<String> tableBuffer, MarkdownDocument doc) {
        if (tableBuffer.isEmpty()) return;

        List<List<List<MarkdownDocument.TextNode>>> rows = new ArrayList<>();
        for (String l : tableBuffer) {
            String inner = l.trim();
            if (inner.startsWith("|")) inner = inner.substring(1);
            if (inner.endsWith("|")) inner = inner.substring(0, inner.length() - 1);
            String[] cells = inner.split("\\|");
            List<String> row = new ArrayList<>();
            for (String c : cells) row.add(c.trim());
            rows.add(row.stream().map(MarkdownParser::parseInline).toList());
        }
        doc.addNode(new MarkdownDocument.TableNode(rows, true));
        tableBuffer.clear();
    }

    // TODO: nested styles, colors
    static List<MarkdownDocument.TextNode> parseInline(String text) {
        List<MarkdownDocument.TextNode> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;

        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '`') {
                // inline code: find next backtick
                int j = text.indexOf('`', i + 1);
                if (j == -1) j = text.length();
                String code = text.substring(i + 1, j);
                out.add(MarkdownDocument.TextNode.builder(code).code().build());
                i = j + 1;
            } else if (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                // bold
                int j = text.indexOf("**", i + 2);
                if (j == -1) j = text.length();
                String inner = text.substring(i + 2, j);
                out.add(MarkdownDocument.TextNode.builder(inner).bold().build());
                i = j + 2;
            } else if (c == '*') {
                // italic
                int j = text.indexOf('*', i + 1);
                if (j == -1) j = text.length();
                String inner = text.substring(i + 1, j);
                out.add(MarkdownDocument.TextNode.builder(inner).italic().build());
                i = j + 1;
            } else {
                // accumulate plain until next special char
                int j = i;
                StringBuilder sb = new StringBuilder();
                while (j < text.length()) {
                    char cc = text.charAt(j);
                    if (cc == '`' || cc == '*') break;
                    sb.append(cc);
                    j++;
                }
                out.add(MarkdownDocument.TextNode.builder(sb.toString()).build());
                i = j;
            }
        }
        return out;
    }
}
