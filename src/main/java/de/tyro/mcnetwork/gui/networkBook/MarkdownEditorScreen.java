package de.tyro.mcnetwork.gui.networkBook;

import de.tyro.mcnetwork.networkBook.data.MarkdownFileManager;
import de.tyro.mcnetwork.networkBook.data.SubTopic;
import de.tyro.mcnetwork.networkBook.data.Topic;
import de.tyro.mcnetwork.networkBook.data.TopicManager;
import de.tyro.mcnetwork.networkBook.markdown.MarkdownDocument;
import de.tyro.mcnetwork.networkBook.markdown.MarkdownParser;
import de.tyro.mcnetwork.networkBook.markdown.MarkdownRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/**
 * In-game editor for markdown/YAML files in network book chapters.
 * Allows editing the content field of YAML files directly in game.
 */
public class MarkdownEditorScreen extends Screen {

    private static final int PADDING = 16;
    private static final int BUTTON_HEIGHT = 20;
    private static final int LINE_HEIGHT = 10;
    private static final int DIVIDER_WIDTH = 2;

    private final Screen previousScreen;
    private final SubTopic editingSubtopic;
    private final Topic editingTopic;

    private String rawMarkdownContent;
    private String[] lines;
    private int scrollY = 0;
    private int previewScrollY = 0;
    private int cursorLine = 0;
    private int cursorColumn = 0;
    private long lastCursorBlink = 0;
    private boolean cursorVisible = true;

    private Button saveButton;
    private Button cancelButton;

    private boolean hasUnsavedChanges = false;

    // Scrollbar dragging
    private boolean scrollbarDragging = false;
    private float scrollbarDragOffset = 0;

    // Preview pane
    private MarkdownDocument previewDocument;
    private MarkdownRenderer previewRenderer;
    private int previewContentHeight = 0;

    public MarkdownEditorScreen(Screen previousScreen, SubTopic subtopic, Topic topic) {
        super(Component.literal("Markdown Editor"));
        this.previousScreen = previousScreen;
        this.editingSubtopic = subtopic;
        this.editingTopic = topic;

        // Find and load the YAML file for this subtopic using its ResourceLocation
        this.rawMarkdownContent = MarkdownFileManager.readMarkdownContent(subtopic.getLocation());
    }

    @Override
    public void init() {
        super.init();

        // Initialize text editor
        if (rawMarkdownContent.isEmpty()) {
            lines = new String[]{""};
        } else {
            lines = rawMarkdownContent.split("\\r?\\n", -1);
        }

        // Initialize preview renderer
        previewRenderer = new MarkdownRenderer();
        updatePreview();

        // Save button
        saveButton = Button.builder(Component.literal("Save"), button -> onSave())
                .pos(this.width - PADDING - 150, this.height - PADDING - BUTTON_HEIGHT)
                .size(70, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(saveButton);

        // Cancel button
        cancelButton = Button.builder(Component.literal("Cancel"), button -> onCancel())
                .pos(this.width - PADDING - 70, this.height - PADDING - BUTTON_HEIGHT)
                .size(60, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(cancelButton);
    }

    /**
     * Updates the preview document by re-parsing the current editor content.
     */
    private void updatePreview() {
        String currentContent = String.join("\n", lines);
        previewDocument = MarkdownParser.parse(currentContent, editingTopic.getContentLocation());
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        super.render(gg, mouseX, mouseY, partialTicks);

        // Draw title bar
        String titleText = "Editing: " + editingSubtopic.getTitle();
        String fileInfo = editingSubtopic.getTitle();
        gg.drawString(this.font, titleText + fileInfo, PADDING, PADDING, 0xFFFFFF);

        // Show save status
        if (hasUnsavedChanges) {
            gg.drawString(this.font, "● Unsaved Changes", this.width - PADDING - 120, PADDING, 0xFFFF6600);
        }

        // Calculate layout
        int topY = PADDING + 24;
        int bottomY = this.height - PADDING - BUTTON_HEIGHT;
        int panelHeight = bottomY - topY - 8;

        int leftEdgeX = PADDING;
        int rightEdgeX = this.width - PADDING;
        int centerX = leftEdgeX + (rightEdgeX - leftEdgeX) / 2;

        int editorWidth = centerX - DIVIDER_WIDTH / 2 - leftEdgeX;
        int previewWidth = rightEdgeX - centerX - DIVIDER_WIDTH / 2;

        // Draw left panel (editor)
        renderEditorPanel(gg, leftEdgeX, topY, editorWidth, panelHeight);

        // Draw divider
        gg.fill(centerX - DIVIDER_WIDTH / 2, topY, centerX + DIVIDER_WIDTH / 2, topY + panelHeight, 0xFF666666);

        // Draw right panel (preview)
        renderPreviewPanel(gg, centerX + DIVIDER_WIDTH / 2, topY, previewWidth, panelHeight);
    }

    /**
     * Wraps a line at word boundaries (whitespace).
     * Returns array of wrapped portions.
     */
    private String[] wrapLineByWords(String line, int contentMaxWidth) {
        int charWidth = this.font.width("W");

        if (contentMaxWidth <= 0 || line.isEmpty()) {
            return new String[]{line};
        }

        java.util.List<String> wrappedParts = new java.util.ArrayList<>();
        String currentLine = "";
        String[] words = line.split("(?<=\\s)|(?=\\s)"); // Split but keep whitespace

        for (String word : words) {
            String testLine = currentLine + word;
            if (this.font.width(testLine) <= contentMaxWidth) {
                currentLine = testLine;
            } else {
                if (!currentLine.isEmpty()) {
                    wrappedParts.add(currentLine);
                }
                currentLine = word;

                // If a single word is too long, force split it
                if (this.font.width(currentLine) > contentMaxWidth) {
                    while (this.font.width(currentLine) > contentMaxWidth && currentLine.length() > 1) {
                        wrappedParts.add(currentLine.substring(0, currentLine.length() - 1));
                        currentLine = currentLine.substring(currentLine.length() - 1);
                    }
                }
            }
        }
        if (!currentLine.isEmpty()) {
            wrappedParts.add(currentLine);
        }

        return wrappedParts.toArray(new String[0]);
    }

    /**
     * Gets the visual line index from logical line.
     */
    private int getVisualLineIndex(int logicalLine, int contentMaxWidth) {
        int visualLine = 0;
        for (int i = 0; i < logicalLine; i++) {
            String[] wrapped = wrapLineByWords(lines[i], contentMaxWidth);
            visualLine += wrapped.length;
        }
        return visualLine;
    }

    /**
     * Converts a visual line index to logical line and wrap offset.
     */
    private int[] visualLineToLogical(int visualLine, int contentMaxWidth) {
        int currentVisualLine = 0;
        for (int logicalLine = 0; logicalLine < lines.length; logicalLine++) {
            String[] wrapped = wrapLineByWords(lines[logicalLine], contentMaxWidth);
            if (currentVisualLine + wrapped.length > visualLine) {
                int wrapOffset = visualLine - currentVisualLine;
                return new int[]{logicalLine, wrapOffset};
            }
            currentVisualLine += wrapped.length;
        }
        return new int[]{lines.length - 1, 0};
    }

    /**
     * Gets total visual lines (wrapped lines).
     */
    private int getTotalVisualLines(int contentMaxWidth) {
        int total = 0;
        for (String line : lines) {
            total += wrapLineByWords(line, contentMaxWidth).length;
        }
        return total;
    }

    /**
     * Gets a specific wrapped portion.
     */
    private String getWrappedPortion(int logicalLine, int wrapOffset, int contentMaxWidth) {
        String[] wrapped = wrapLineByWords(lines[logicalLine], contentMaxWidth);
        if (wrapOffset >= 0 && wrapOffset < wrapped.length) {
            return wrapped[wrapOffset];
        }
        return "";
    }

    /**
     * Renders the editor panel (left side).
     */
    private void renderEditorPanel(GuiGraphics gg, int x, int y, int width, int height) {
        // Background
        gg.fill(x, y, x + width, y + height, 0xFF222228);
        gg.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF666666);

        // Draw line numbers and content
        gg.enableScissor(x, y, x + width, y + height);

        int contentMaxWidth = width - 40;
        int visibleVisualLines = height / LINE_HEIGHT;

        int startVisualLine = scrollY;
        int endVisualLine = startVisualLine + visibleVisualLines + 2;

        for (int visualLine = startVisualLine; visualLine < endVisualLine; visualLine++) {
            if (visualLine < 0) continue;

            int[] logicalInfo = visualLineToLogical(visualLine, contentMaxWidth);
            int logicalLine = logicalInfo[0];
            int wrapOffset = logicalInfo[1];

            if (logicalLine >= lines.length) break;

            int screenY = y + (visualLine - startVisualLine) * LINE_HEIGHT;
            if (screenY >= y + height) break;

            // Draw line number only on first visual line of each logical line
            if (wrapOffset == 0) {
                gg.drawString(this.font, String.format("%3d", logicalLine + 1), x + 4, screenY, 0xFF888888);
            }

            // Draw wrapped content
            String wrappedPart = getWrappedPortion(logicalLine, wrapOffset, contentMaxWidth);

            if (!wrappedPart.isEmpty()) {
                gg.drawString(this.font, wrappedPart, x + 32, screenY, 0xFFFFFFFF);
            }

            // Draw cursor on the correct visual line
            if (logicalLine == cursorLine && cursorVisible) {
                String[] wrapped = wrapLineByWords(lines[logicalLine], contentMaxWidth);
                int columnStart = 0;
                for (int w = 0; w < wrapOffset && w < wrapped.length; w++) {
                    columnStart += wrapped[w].length();
                }
                int columnEnd = columnStart + wrappedPart.length();

                if (cursorColumn >= columnStart && cursorColumn <= columnEnd) {
                    String beforeCursor = wrappedPart.substring(0, Math.min(cursorColumn - columnStart, wrappedPart.length()));
                    int cursorX = x + 32 + this.font.width(beforeCursor);
                    gg.fill(cursorX, screenY, cursorX + 2, screenY + LINE_HEIGHT, 0xFFFFFFFF);
                }
            }
        }

        gg.disableScissor();

        // Draw scrollbar
        int totalVisualLines = getTotalVisualLines(contentMaxWidth);
        if (totalVisualLines > visibleVisualLines) {
            int scrollbarX = x + width - 8;
            float scrollPercentage = (float) scrollY / (totalVisualLines - visibleVisualLines);
            int thumbHeight = Math.max(20, (int) (height * ((float) visibleVisualLines / totalVisualLines)));
            int thumbY = y + (int) (scrollPercentage * (height - thumbHeight));

            gg.fill(scrollbarX, y, scrollbarX + 8, y + height, 0xFF333333);
            gg.fill(scrollbarX, thumbY, scrollbarX + 8, thumbY + thumbHeight, 0xFF888888);
        }

        // Draw status bar
        int statusY = y + height + 4;
        gg.drawString(this.font, "Ln " + (cursorLine + 1) + ", Col " + (cursorColumn + 1), x, statusY, 0xFFAAAAAA);
    }

    /**
     * Renders the preview panel (right side).
     */
    private void renderPreviewPanel(GuiGraphics gg, int x, int y, int width, int height) {
        // Background
        gg.fill(x, y, x + width, y + height, 0xFF1a1a1e);
        gg.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF666666);

        // Render preview content
        if (previewDocument != null && previewRenderer != null) {
            gg.enableScissor(x, y, x + width, y + height);

            gg.pose().pushPose();
            gg.pose().translate(0, -previewScrollY, 0);

            previewContentHeight = previewRenderer.render(
                    gg, previewDocument,
                    x + 8, y + 8, width - 16,
                    x + 8, y + 8, width - 24, height - 16
            );

            gg.pose().popPose();
            gg.disableScissor();

            // Draw scrollbar for preview
            if (previewContentHeight > height) {
                int scrollbarX = x + width - 8;

                float scrollPercentage = (float) previewScrollY / (previewContentHeight - height);
                int thumbHeight = Math.max(20, (int) (height * ((float) height / previewContentHeight)));
                int thumbY = y + (int) (scrollPercentage * (height - thumbHeight));

                // Draw scrollbar background
                gg.fill(scrollbarX, y, scrollbarX + 8, y + height, 0xFF333333);
                // Draw scrollbar thumb
                gg.fill(scrollbarX, thumbY, scrollbarX + 8, thumbY + thumbHeight, 0xFF888888);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int topY = PADDING + 24;
        int bottomY = this.height - PADDING - BUTTON_HEIGHT;
        int centerX = PADDING + (this.width - 2 * PADDING) / 2;

        if (mouseY >= topY && mouseY < bottomY) {
            if (mouseX < centerX) {
                // Scroll left panel (editor) - scroll by visual lines
                int editorHeight = bottomY - topY - 8;
                int editorWidth = centerX - DIVIDER_WIDTH / 2 - PADDING;
                int contentMaxWidth = editorWidth - 40;

                int visibleVisualLines = editorHeight / LINE_HEIGHT;
                int totalVisualLines = getTotalVisualLines(contentMaxWidth);

                int scrollAmount = (int) (scrollY * 3);
                this.scrollY = Math.max(0, this.scrollY - scrollAmount);
                int maxScroll = Math.max(0, totalVisualLines - visibleVisualLines);
                this.scrollY = Math.min(this.scrollY, maxScroll);

                return true;
            } else {
                // Scroll right panel (preview)
                int panelHeight = bottomY - topY - 8;

                int scrollAmount = (int) (scrollY * 10);
                this.previewScrollY = Math.max(0, this.previewScrollY - scrollAmount);

                int maxScroll = Math.max(0, previewContentHeight - panelHeight);
                this.previewScrollY = Math.min(this.previewScrollY, maxScroll);

                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int topY = PADDING + 24;
        int bottomY = this.height - PADDING - BUTTON_HEIGHT;
        int centerX = PADDING + (this.width - 2 * PADDING) / 2;

        int editorX = PADDING;
        int editorHeight = bottomY - topY - 8;
        int editorWidth = centerX - DIVIDER_WIDTH / 2 - PADDING;
        int contentMaxWidth = editorWidth - 40;

        if (mouseX >= editorX && mouseX < centerX && mouseY >= topY && mouseY < bottomY) {
            int visibleVisualLines = editorHeight / LINE_HEIGHT;
            int totalVisualLines = getTotalVisualLines(contentMaxWidth);
            int scrollbarX = editorX + editorWidth - 8;

            // Check for scrollbar interaction
            if (totalVisualLines > visibleVisualLines) {
                float scrollPercentage = (float) scrollY / (totalVisualLines - visibleVisualLines);
                int thumbHeight = Math.max(20, (int) (editorHeight * ((float) visibleVisualLines / totalVisualLines)));
                int thumbY = topY + (int) (scrollPercentage * (editorHeight - thumbHeight));

                // Check if clicking on scrollbar thumb
                if (mouseX >= scrollbarX && mouseX < scrollbarX + 8 &&
                        mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
                    scrollbarDragging = true;
                    scrollbarDragOffset = (float) (mouseY - thumbY);
                    return true;
                }

                // Check if clicking on scrollbar track
                if (mouseX >= scrollbarX && mouseX < scrollbarX + 8) {
                    float clickPercentage = (float) (mouseY - topY) / (editorHeight - thumbHeight);
                    scrollY = (int) (clickPercentage * (totalVisualLines - visibleVisualLines));
                    scrollY = Math.max(0, Math.min(scrollY, totalVisualLines - visibleVisualLines));
                    return true;
                }
            }

            // Click on editor text area
            if (mouseX < scrollbarX) {
                int relativeY = (int) (mouseY - topY);
                int visualLine = relativeY / LINE_HEIGHT + scrollY;

                if (visualLine >= 0) {
                    int[] logicalInfo = visualLineToLogical(visualLine, contentMaxWidth);
                    int logicalLine = logicalInfo[0];
                    int wrapOffset = logicalInfo[1];

                    if (logicalLine < lines.length) {
                        cursorLine = logicalLine;

                        // Get the wrapped portion to calculate column offset
                        String wrappedPart = getWrappedPortion(logicalLine, wrapOffset, contentMaxWidth);
                        String[] wrapped = wrapLineByWords(lines[logicalLine], contentMaxWidth);

                        // Calculate column start
                        int columnStart = 0;
                        for (int w = 0; w < wrapOffset && w < wrapped.length; w++) {
                            columnStart += wrapped[w].length();
                        }

                        // Calculate precise column position
                        double relativeX = mouseX - editorX - 32;
                        if (relativeX < 0) relativeX = 0;

                        int estimatedOffset = 0;
                        for (int i = 0; i < wrappedPart.length(); i++) {
                            int charWidth = this.font.width(wrappedPart.substring(0, i + 1));
                            if (charWidth > relativeX) break;
                            estimatedOffset = i + 1;
                        }

                        cursorColumn = columnStart + estimatedOffset;
                        cursorColumn = Math.max(0, Math.min(cursorColumn, lines[logicalLine].length()));

                        lastCursorBlink = System.currentTimeMillis();
                        cursorVisible = true;
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onCancel();
            return true;
        }

        //Shift pressed
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            if (keyCode == GLFW.GLFW_KEY_S) {
                onSave();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                //delete until next whitespace or beginning of the line
                if (cursorColumn > 0) {
                    String line = lines[cursorLine];
                    var prevWhitespace = lines[cursorLine].substring(0, cursorColumn).lastIndexOf(' ');
                    if (prevWhitespace == -1) prevWhitespace = 0;
                    var newLine = line.substring(0, prevWhitespace);
                    if (cursorColumn < lines[cursorLine].length()) newLine += line.substring(cursorColumn + 1);
                    lines[cursorLine] = newLine;
                    hasUnsavedChanges = true;
                    cursorColumn -= cursorColumn - prevWhitespace;
                    updatePreview();
                } else if (cursorLine > 0) {
                    lines[cursorLine] = lines[cursorLine - 1] + lines[cursorLine];
                    lines = removeIndex(lines, cursorLine);
                    cursorLine -= 1;
                    cursorColumn = lines[cursorLine].length();
                    hasUnsavedChanges = true;
                    updatePreview();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_V) {
                var line = lines[cursorLine];
                var insert = Minecraft.getInstance().keyboardHandler.getClipboard();
                lines[cursorLine] = line.substring(0, cursorColumn) + insert + line.substring(cursorColumn);
                cursorColumn += insert.length();
            }
        }


        // Get content max width for wrapped line navigation
        int editorWidth = (this.width / 2) - DIVIDER_WIDTH / 2 - PADDING;
        int contentMaxWidth = editorWidth - 40;

        // Arrow keys - navigate by visual lines
        if (keyCode == GLFW.GLFW_KEY_UP) {
            // Find current visual line
            int currentVisualLine = getVisualLineIndex(cursorLine, contentMaxWidth);
            String[] wrapped;
            try {
                wrapped = wrapLineByWords(lines[cursorLine], contentMaxWidth);
            } catch (IndexOutOfBoundsException e) {
                return true;
            }

            // Find which wrap section cursor is in
            int columnStart = 0;
            int wrapOffset = 0;
            for (int w = 0; w < wrapped.length; w++) {
                if (cursorColumn <= columnStart + wrapped[w].length()) {
                    wrapOffset = w;
                    break;
                }
                columnStart += wrapped[w].length();
            }

            int visualLine = currentVisualLine + wrapOffset - 1;
            if (visualLine >= 0) {
                int[] logicalInfo = visualLineToLogical(visualLine, contentMaxWidth);
                cursorLine = logicalInfo[0];

                // Try to maintain column position
                String[] targetWrapped = wrapLineByWords(lines[cursorLine], contentMaxWidth);
                int newWrapOffset = logicalInfo[1];
                int newColumnStart = 0;
                for (int w = 0; w < newWrapOffset && w < targetWrapped.length; w++) {
                    newColumnStart += targetWrapped[w].length();
                }
                int newColumnEnd = newColumnStart + targetWrapped[newWrapOffset].length();
                int newColumn = Math.min(cursorColumn - columnStart + newColumnStart, newColumnEnd);
                cursorColumn = Math.max(newColumnStart, Math.min(newColumn, lines[cursorLine].length()));
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            // Find current visual line
            int currentVisualLine = getVisualLineIndex(cursorLine, contentMaxWidth);
            String[] wrapped;
            try {
                wrapped = wrapLineByWords(lines[cursorLine], contentMaxWidth);
            } catch (IndexOutOfBoundsException e) {
                return true;
            }

            // Find which wrap section cursor is in
            int columnStart = 0;
            int wrapOffset = 0;
            for (int w = 0; w < wrapped.length; w++) {
                if (cursorColumn <= columnStart + wrapped[w].length()) {
                    wrapOffset = w;
                    break;
                }
                columnStart += wrapped[w].length();
            }

            int visualLine = currentVisualLine + wrapOffset + 1;
            int totalVisualLines = getTotalVisualLines(contentMaxWidth);
            if (visualLine < totalVisualLines) {
                int[] logicalInfo = visualLineToLogical(visualLine, contentMaxWidth);
                cursorLine = logicalInfo[0];

                // Try to maintain column position
                String[] targetWrapped = wrapLineByWords(lines[cursorLine], contentMaxWidth);
                int newWrapOffset = logicalInfo[1];
                int newColumnStart = 0;
                for (int w = 0; w < newWrapOffset && w < targetWrapped.length; w++) {
                    newColumnStart += targetWrapped[w].length();
                }
                int newColumnEnd = newColumnStart + targetWrapped[newWrapOffset].length();
                int newColumn = Math.min(cursorColumn - columnStart + newColumnStart, newColumnEnd);
                cursorColumn = Math.max(newColumnStart, Math.min(newColumn, lines[cursorLine].length()));
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (cursorColumn > 0) cursorColumn--;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (cursorColumn < lines[cursorLine].length()) cursorColumn++;
            return true;
        }

        // Home / End
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursorColumn = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            cursorColumn = lines[cursorLine].length();
            return true;
        }

        // Page Up / Down - navigate by visual lines
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            int currentVisualLine = getVisualLineIndex(cursorLine, contentMaxWidth);
            int editorHeight = (this.height - PADDING * 3 - BUTTON_HEIGHT - 24);
            int visibleVisualLines = editorHeight / LINE_HEIGHT;
            int visualLine = Math.max(0, currentVisualLine - visibleVisualLines);
            int[] logicalInfo = visualLineToLogical(visualLine, contentMaxWidth);
            cursorLine = logicalInfo[0];
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            int currentVisualLine = getVisualLineIndex(cursorLine, contentMaxWidth);
            int editorHeight = (this.height - PADDING * 3 - BUTTON_HEIGHT - 24);
            int visibleVisualLines = editorHeight / LINE_HEIGHT;
            int totalVisualLines = getTotalVisualLines(contentMaxWidth);
            int visualLine = Math.min(totalVisualLines - 1, currentVisualLine + visibleVisualLines);
            int[] logicalInfo = visualLineToLogical(visualLine, contentMaxWidth);
            cursorLine = logicalInfo[0];
            return true;
        }

        // Backspace, Delete, Enter, Tab
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (cursorColumn > 0) {
                String line = lines[cursorLine];
                lines[cursorLine] = line.substring(0, cursorColumn - 1) + line.substring(cursorColumn);
                cursorColumn--;
                hasUnsavedChanges = true;
                updatePreview();
            } else if (cursorLine > 0) {
                String currentLine = lines[cursorLine];
                lines[cursorLine - 1] = lines[cursorLine - 1] + currentLine;
                cursorColumn = lines[cursorLine - 1].length() - currentLine.length();
                lines = removeIndex(lines, cursorLine);
                cursorLine--;
                hasUnsavedChanges = true;
                updatePreview();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursorColumn < lines[cursorLine].length()) {
                String line = lines[cursorLine];
                lines[cursorLine] = line.substring(0, cursorColumn) + line.substring(cursorColumn + 1);
                hasUnsavedChanges = true;
                updatePreview();
            } else if (cursorLine < lines.length - 1) {
                lines[cursorLine] = lines[cursorLine] + lines[cursorLine + 1];
                lines = removeIndex(lines, cursorLine + 1);
                hasUnsavedChanges = true;
                updatePreview();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            String line = lines[cursorLine];
            String before = line.substring(0, cursorColumn);
            String after = line.substring(cursorColumn);
            lines[cursorLine] = before;
            lines = insertIndex(lines, cursorLine + 1, after);
            cursorLine++;
            cursorColumn = 0;
            hasUnsavedChanges = true;
            updatePreview();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_TAB) {
            String line = lines[cursorLine];
            lines[cursorLine] = line.substring(0, cursorColumn) + "  " + line.substring(cursorColumn);
            cursorColumn += 2;
            hasUnsavedChanges = true;
            updatePreview();
            return true;
        }

        lastCursorBlink = System.currentTimeMillis();
        cursorVisible = true;

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (codePoint >= 32) { // Printable characters
            String line = lines[cursorLine];
            lines[cursorLine] = line.substring(0, cursorColumn) + codePoint + line.substring(cursorColumn);
            cursorColumn++;
            hasUnsavedChanges = true;
            updatePreview();

            lastCursorBlink = System.currentTimeMillis();
            cursorVisible = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbarDragging) {
            int editorY = PADDING + 24;
            int editorHeight = this.height - PADDING * 3 - BUTTON_HEIGHT - 24;
            int editorWidth = (this.width / 2) - DIVIDER_WIDTH / 2 - PADDING;
            int contentMaxWidth = editorWidth - 40;

            int visibleVisualLines = editorHeight / LINE_HEIGHT;
            int totalVisualLines = getTotalVisualLines(contentMaxWidth);
            int thumbHeight = Math.max(20, (int) (editorHeight * ((float) visibleVisualLines / totalVisualLines)));
            int trackRange = editorHeight - thumbHeight;

            if (trackRange > 0) {
                float thumbTop = (float) (mouseY - editorY - scrollbarDragOffset);
                thumbTop = Math.max(0, Math.min(thumbTop, trackRange));

                float percentage = thumbTop / trackRange;
                scrollY = (int) (percentage * (totalVisualLines - visibleVisualLines));
                scrollY = Math.max(0, Math.min(scrollY, totalVisualLines - visibleVisualLines));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollbarDragging) {
            scrollbarDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        super.tick();
        // Blink cursor
        long now = System.currentTimeMillis();
        if (now - lastCursorBlink > 500) {
            cursorVisible = !cursorVisible;
            lastCursorBlink = now;
        }
    }

    private void onSave() {
        rawMarkdownContent = String.join("\n", lines);

        // Save the content back to the YAML file
        boolean success = MarkdownFileManager.writeMarkdownContent(editingSubtopic.getLocation(), rawMarkdownContent);
        if (success) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            hasUnsavedChanges = false;

            // Reload the topics to reflect changes
            TopicManager.getInstance().reloadTopic(editingTopic);
        } else {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_TAKE_RESULT, 1.0f));
        }

    }

    private void onCancel() {
        if (hasUnsavedChanges) {
            // TODO: Show confirmation dialog
        }
        Minecraft.getInstance().setScreen(previousScreen);
    }

    @Override
    public void onClose() {
        if (hasUnsavedChanges) {
            // TODO: Show confirmation dialog
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    // Helper methods
    private String[] removeIndex(String[] arr, int index) {
        String[] newArr = new String[arr.length - 1];
        System.arraycopy(arr, 0, newArr, 0, index);
        System.arraycopy(arr, index + 1, newArr, index, arr.length - index - 1);
        return newArr;
    }

    private String[] insertIndex(String[] arr, int index, String value) {
        String[] newArr = new String[arr.length + 1];
        System.arraycopy(arr, 0, newArr, 0, index);
        newArr[index] = value;
        System.arraycopy(arr, index, newArr, index + 1, arr.length - index);
        return newArr;
    }
}





