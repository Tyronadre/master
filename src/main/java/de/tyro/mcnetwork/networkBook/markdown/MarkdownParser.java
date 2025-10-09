package de.tyro.mcnetwork.networkBook.markdown;

import de.tyro.mcnetwork.MCNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.Collection;
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
    private static final Pattern animationPatter = Pattern.compile("@animation\\[(.*)]");

    private static final ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

    public static MarkdownRenderer.ParsedDocument parse(String markdown, ResourceLocation resourceLocation) {
        MarkdownRenderer.ParsedDocument doc = new MarkdownRenderer.ParsedDocument();

        String[] lines = markdown.split("\\r?\\n", -1);
        int i = 0;
        boolean inCode = false;
        StringBuilder codeBuf = new StringBuilder();
        String codeLang = null;
        List<String> tableBuffer = new ArrayList<>();
        List<String> listBuffer = new ArrayList<>();

        while (i < lines.length) {
            String line = lines[i];

            //image
            Matcher img = imagePattern.matcher(line);
            if (img.find()) {
                doc.blocks.add(new MarkdownRenderer.ImageBlock(resourceLocation.withSuffix("/" + img.group(1)), img.group(2)));
                i++;
                continue;
            }

            Matcher animation = animationPatter.matcher(line);
            if (animation.find()) {
                doc.blocks.add(new MarkdownRenderer.AnimationBlock(resourceLocation.withSuffix("/" +animation.group(1) + ".json")));
                i++;
                continue;
            }

            // ordered list
            Matcher oList = orderedListItemPattern.matcher(line);
            if (oList.matches()) {
                listBuffer.add(oList.group(1));
                i++;
                // collect following ordered list items
                while (i < lines.length) {
                    Matcher oList2 = orderedListItemPattern.matcher(lines[i]);
                    if (oList2.matches()) {
                        listBuffer.add(oList2.group(1));
                        i++;
                    } else break;
                }
                flushListBuffer(listBuffer, doc, true);
                continue;
            }


            // Fenced code block handling
            Matcher fence = fencedCodePattern.matcher(line);
            if (fence.matches()) {
                if (!inCode) {
                    inCode = true;
                    codeBuf.setLength(0);
                    codeLang = fence.group(1);
                } else {
                    // end fenced code
                    doc.blocks.add(new MarkdownRenderer.CodeBlock(codeBuf.toString(), codeLang));
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

            // horizontal rule
            if (hrPattern.matcher(line).matches()) {
                flushTableBuffer(tableBuffer, doc);
                doc.blocks.add(new MarkdownRenderer.HrBlock());
                i++;
                continue;
            }

            // heading
            Matcher hm = headingPattern.matcher(line);
            if (hm.matches()) {
                flushTableBuffer(tableBuffer, doc);
                int level = hm.group(1).length();
                String text = hm.group(2).trim();
                doc.blocks.add(new MarkdownRenderer.Heading(level, text));
                i++;
                continue;
            }

            // table detection: sequence of lines with |...|
            if (tableLinePattern.matcher(line).matches()) {
                // collect table lines until break
                tableBuffer.add(line.trim());
                i++;
                // continue collecting
                while (i < lines.length && tableLinePattern.matcher(lines[i]).matches()) {
                    tableBuffer.add(lines[i].trim());
                    i++;
                }
                // flush now
                flushTableBuffer(tableBuffer, doc);
                continue;
            }

            // list item
            Matcher li = listItemPattern.matcher(line);
            if (li.matches()) {
                listBuffer.add(li.group(1));
                i++;
                // collect following list items
                while (i < lines.length) {
                    Matcher li2 = listItemPattern.matcher(lines[i]);
                    if (li2.matches()) {
                        listBuffer.add(li2.group(1));
                        i++;
                    } else break;
                }
                flushListBuffer(listBuffer, doc, true);
                continue;
            }

            // blank line -> paragraph separator
            if (line.trim().isEmpty()) {
                i++;
                continue;
            }

            // otherwise paragraph: collect until blank or other block marker
            StringBuilder para = new StringBuilder();
            para.append(line);
            i++;
            while (i < lines.length && !lines[i].trim().isEmpty() && !headingPattern.matcher(lines[i]).matches() && !listItemPattern.matcher(lines[i]).matches() && !tableLinePattern.matcher(lines[i]).matches() && !hrPattern.matcher(lines[i]).matches() && !fencedCodePattern.matcher(lines[i]).matches()) {
                para.append("\n").append(lines[i]);
                i++;
            }
            doc.blocks.add(new MarkdownRenderer.Paragraph(parseInline(para.toString())));
        }

        // flush any remaining buffers
        flushTableBuffer(tableBuffer, doc);

        return doc;
    }

    private static void flushTableBuffer(List<String> tableBuffer, MarkdownRenderer.ParsedDocument doc) {
        if (tableBuffer.isEmpty()) return;
        // parse tableBuffer lines into rows (split by |, trim)
        List<List<String>> rows = new ArrayList<>();
        for (String l : tableBuffer) {
            // remove leading/trailing '|'
            String inner = l.trim();
            if (inner.startsWith("|")) inner = inner.substring(1);
            if (inner.endsWith("|")) inner = inner.substring(0, inner.length() - 1);
            String[] cells = inner.split("\\|");
            List<String> row = new ArrayList<>();
            for (String c : cells) row.add(c.trim());
            rows.add(row);
        }
        boolean header = rows.size() > 0;
        doc.blocks.add(new MarkdownRenderer.TableBlock(rows, header));
        tableBuffer.clear();
    }

    private static void flushListBuffer(List<String> listBuffer, MarkdownRenderer.ParsedDocument doc, boolean ordered) {
        if (listBuffer.isEmpty()) return;
        MarkdownRenderer.ListBlock lb = new MarkdownRenderer.ListBlock(listBuffer.stream().map(MarkdownParser::parseInline).toList(), ordered);
        doc.blocks.add(lb);
        listBuffer.clear();
    }

    // ----- inline parsing: supports **bold**, *italic*, `code` -----
    public static List<MarkdownRenderer.InlineNode> parseInline(String text) {
        List<MarkdownRenderer.InlineNode> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;

        // We implement a small state machine scanning for backticks, **, *
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '`') {
                // inline code: find next backtick
                int j = text.indexOf('`', i + 1);
                if (j == -1) j = text.length();
                String code = text.substring(i + 1, j);
                out.add(new MarkdownRenderer.InlineCodeNode(code));
                i = j + 1;
                continue;
            } else if (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                // bold
                int j = text.indexOf("**", i + 2);
                if (j == -1) j = text.length();
                String inner = text.substring(i + 2, j);
                out.add(new MarkdownRenderer.BoldNode(inner));
                i = j + 2;
                continue;
            } else if (c == '*') {
                // italic
                int j = text.indexOf('*', i + 1);
                if (j == -1) j = text.length();
                String inner = text.substring(i + 1, j);
                out.add(new MarkdownRenderer.ItalicNode(inner));
                i = j + 1;
                continue;
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
                out.add(new MarkdownRenderer.TextNode(sb.toString()));
                i = j;
            }
        }
        return out;
    }
}
