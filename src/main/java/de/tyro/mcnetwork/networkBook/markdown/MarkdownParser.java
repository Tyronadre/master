package de.tyro.mcnetwork.networkBook.markdown;

import de.tyro.mcnetwork.networkBook.markdown.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

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
    private static final Pattern animationPatter = Pattern.compile("@animation\\[(.*)]");

    private static final ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

    public static MarkdownDocument parse(String markdown, ResourceLocation resourceLocation) {
        var doc = new MarkdownDocument();

        String[] lines = markdown.split("\\r?\\n", -1);
        int i = 0;
        boolean inCode = false;
        StringBuilder codeBuf = new StringBuilder();
        String codeLang = null;
        List<String> tableBuffer = new ArrayList<>();
        List<String> listBuffer = new ArrayList<>();

        while (i < lines.length) {
            String line = lines[i];

            Matcher codeBlock = fencedCodePattern.matcher(line);
            if (codeBlock.matches()) {
                if (!inCode) {
                    inCode = true;
                    codeBuf.setLength(0);
                    codeLang = codeBlock.group(1);
                } else {
                    doc.addCodeBlock(codeBuf.toString(), codeLang);
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

            //task
            if (line.trim().startsWith("@task[")) {
                StringBuilder taskContent = new StringBuilder();
                String startLine = line.trim();
                
                if (startLine.endsWith("]")) {
                    // Single line task
                    String content = startLine.substring(6, startLine.length() - 1); // remove @task[ and ]
                    String[] pairs = content.split(";");
                    List<List<Block>> questions = new ArrayList<>();
                    List<String> answers = new ArrayList<>();
                    for (String pair : pairs) {
                        String[] qa = pair.split("\\|");
                        if (qa.length == 2) {
                            questions.add(parse(qa[0], resourceLocation).blocks);
                            answers.add(qa[1]);
                        }
                    }
                    doc.addTaskBlock(questions, answers);
                    i++;
                    continue;
                }
                
                // Multiline task - use bracket counting
                int bracketCount = 1; // We start with one open bracket from @task[
                taskContent.append(startLine.substring(6)); // remove @task[
                i++;

                // Collect lines until we find the matching closing bracket
                while (i < lines.length && bracketCount > 0) {
                    String currentLine = lines[i];
                    for (char c : currentLine.toCharArray()) {
                        if (c == '[') bracketCount++;
                        else if (c == ']') bracketCount--;
                    }

                    if (bracketCount > 0) {
                        taskContent.append("\n").append(currentLine);
                    } else {
                        // Found the matching closing bracket - add everything before it
                        int lastBracketIndex = currentLine.lastIndexOf(']');
                        if (lastBracketIndex > 0) {
                            taskContent.append("\n").append(currentLine.substring(0, lastBracketIndex));
                        }
                    }
                    i++;
                }

                // Process the collected content
                String content = taskContent.toString();
                String[] pairs = content.split(";");
                List<List<Block>> questions = new ArrayList<>();
                List<String> answers = new ArrayList<>();
                for (String pair : pairs) {
                    String[] qa = pair.split("\\|");
                    if (qa.length == 2) {
                        questions.add(parse(qa[0], resourceLocation).blocks);
                        answers.add(qa[1]);
                    }
                }
                doc.addTaskBlock(questions, answers);
                continue;
            }

            //image
            Matcher img = imagePattern.matcher(line);
            if (img.find()) {
                doc.addImageBlock(resourceLocation.withSuffix("/" + img.group(1)), img.group(2));
                i++;
                continue;
            }

            Matcher animation = animationPatter.matcher(line);
            if (animation.find()) {
                doc.addAnimationBlock(resourceLocation.withSuffix("/" +animation.group(1) + ".json"));
                i++;
                continue;
            }



            Matcher oList = orderedListItemPattern.matcher(line);
            if (oList.matches()) {
                listBuffer.add(oList.group(1));
                i++;
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
                flushListBuffer(listBuffer, doc, false);
                continue;
            }

            if (hrPattern.matcher(line).matches()) {
                flushTableBuffer(tableBuffer, doc);
                doc.addHrBlock();
                i++;
                continue;
            }

            Matcher headerBlock = headingPattern.matcher(line);
            if (headerBlock.matches()) {
                flushTableBuffer(tableBuffer, doc);
                int level = headerBlock.group(1).length();
                String text = headerBlock.group(2).trim();
                doc.addHeaderBlock(level, text);
                i++;
                continue;
            }

            if (tableLinePattern.matcher(line).matches()) {
                tableBuffer.add(line.trim());
                i++;
                while (i < lines.length && tableLinePattern.matcher(lines[i]).matches()) {
                    tableBuffer.add(lines[i].trim());
                    i++;
                }
                flushTableBuffer(tableBuffer, doc);
                continue;
            }

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
            doc.addParagraphBlock(parseInline(para.toString()));
        }

        flushTableBuffer(tableBuffer, doc);

        return doc;
    }

    private static void flushTableBuffer(List<String> tableBuffer, MarkdownDocument doc) {
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
        boolean header = !rows.isEmpty();
        doc.addTableBlock(rows, header);
        tableBuffer.clear();
    }

    private static void flushListBuffer(List<String> listBuffer, MarkdownDocument doc, boolean ordered) {
        if (listBuffer.isEmpty()) return;
        doc.addListBlock(listBuffer.stream().map(MarkdownParser::parseInline).toList(), ordered);
        listBuffer.clear();
    }

    // ----- inline parsing: supports **bold**, *italic*, `code` -----
    public static List<Block.InlineNode> parseInline(String text) {
        List<Block.InlineNode> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;

        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '`') {
                // inline code: find next backtick
                int j = text.indexOf('`', i + 1);
                if (j == -1) j = text.length();
                String code = text.substring(i + 1, j);
                out.add(new Block.InlineCodeNode(code));
                i = j + 1;
            } else if (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                // bold
                int j = text.indexOf("**", i + 2);
                if (j == -1) j = text.length();
                String inner = text.substring(i + 2, j);
                out.add(new Block.BoldNode(inner));
                i = j + 2;
            } else if (c == '*') {
                // italic
                int j = text.indexOf('*', i + 1);
                if (j == -1) j = text.length();
                String inner = text.substring(i + 1, j);
                out.add(new Block.ItalicNode(inner));
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
                out.add(new Block.TextNode(sb.toString()));
                i = j;
            }
        }
        return out;
    }
}
