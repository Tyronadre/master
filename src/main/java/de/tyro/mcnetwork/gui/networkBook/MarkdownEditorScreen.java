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

import java.util.LinkedList;

/**
 * In-game editor for markdown/YAML files in network book chapters.
 * Allows editing the content field of YAML files directly in game.
 */
public class MarkdownEditorScreen extends Screen {

    private static final int PADDING = 16;
    private static final int BUTTON_HEIGHT = 20;
    private static final int LINE_HEIGHT = 10;
    private static final int DIVIDER_WIDTH = 2;

    private static final int MAX_UNDO_HISTORY = 100;
    private static final long DOUBLE_CLICK_INTERVAL = 300; // milliseconds
    private static final int DOUBLE_CLICK_TOLERANCE = 5; // pixels
    // Undo/Redo manager
    private final LinkedList<EditorState> undoStack = new LinkedList<>();

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
    private final LinkedList<EditorState> redoStack = new LinkedList<>();
    // Text selection
    private int selectionStartLine = -1;
    private int selectionStartColumn = -1;
    private int selectionEndLine = -1;
    private int selectionEndColumn = -1;

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
    private boolean isSelecting = false;
    // Confirmation dialog state
    private boolean showingConfirmDialog = false;
    private Runnable confirmAction = null;
    // Multi-click tracking
    private long lastClickTime = 0;
    private int lastClickCount = 0;
    private int lastClickX = -1;
    private int lastClickY = -1;

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
        gg.fill(centerX - DIVIDER_WIDTH / 2, topY, centerX + DIVIDER_WIDTH / 2, topY + panelHeight, 0xFF888888);

        // Draw right panel (preview)
        renderPreviewPanel(gg, centerX + DIVIDER_WIDTH / 2, topY, previewWidth, panelHeight);

        // Draw confirmation dialog if needed
        if (showingConfirmDialog && hasUnsavedChanges) {
            renderBlurredBackground(1);
            renderConfirmationDialog(gg, mouseX, mouseY);
        }
    }

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

    /**
     * Renders the confirmation dialog
     */
    private void renderConfirmationDialog(GuiGraphics gg, int mouseX, int mouseY) {
        int dialogWidth = 320;
        int dialogHeight = 140;
        int dialogX = (this.width - dialogWidth) / 2;
        int dialogY = (this.height - dialogHeight) / 2;

        // Darken background
        gg.fill(0, 0, this.width, this.height, 0x80000000);

        // Dialog background
        gg.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xFF1a1a1e);
        // Dialog border
        gg.fill(dialogX - 1, dialogY - 1, dialogX + dialogWidth + 1, dialogY + dialogHeight + 1, 0xFF666666);

        // Title
        gg.drawCenteredString(this.font, "Unsaved Changes", dialogX + dialogWidth / 2, dialogY + 12, 0xFFFFFF);

        // Message
        gg.drawCenteredString(this.font, "Do you want to discard", dialogX + dialogWidth / 2, dialogY + 40, 0xFFAAAAAA);
        gg.drawCenteredString(this.font, "your unsaved changes?", dialogX + dialogWidth / 2, dialogY + 52, 0xFFAAAAAA);

        // Buttons
        int buttonWidth = 80;
        int buttonHeight = 20;
        int buttonY = dialogY + dialogHeight - 32;
        int buttonSpacing = 12;
        int totalButtonWidth = (buttonWidth * 2) + buttonSpacing;
        int buttonsStartX = dialogX + (dialogWidth - totalButtonWidth) / 2;

        // Save button
        int saveButtonX = buttonsStartX;
        int saveButtonY = buttonY;
        gg.fill(saveButtonX, saveButtonY, saveButtonX + buttonWidth, saveButtonY + buttonHeight, 0xFF4CAF50);
        if (mouseX >= saveButtonX && mouseX < saveButtonX + buttonWidth && mouseY >= saveButtonY && mouseY < saveButtonY + buttonHeight) {
            gg.fill(saveButtonX, saveButtonY, saveButtonX + buttonWidth, saveButtonY + buttonHeight, 0xFF66BB6A);
        }
        gg.drawCenteredString(this.font, "Save", saveButtonX + buttonWidth / 2, saveButtonY + 6, 0xFFFFFFFF);

        // Discard button
        int discardButtonX = buttonsStartX + buttonWidth + buttonSpacing;
        int discardButtonY = buttonY;
        gg.fill(discardButtonX, discardButtonY, discardButtonX + buttonWidth, discardButtonY + buttonHeight, 0xFFf44336);
        if (mouseX >= discardButtonX && mouseX < discardButtonX + buttonWidth && mouseY >= discardButtonY && mouseY < discardButtonY + buttonHeight) {
            gg.fill(discardButtonX, discardButtonY, discardButtonX + buttonWidth, discardButtonY + buttonHeight, 0xFFEF5350);
        }
        gg.drawCenteredString(this.font, "Discard", discardButtonX + buttonWidth / 2, discardButtonY + 6, 0xFFFFFFFF);
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

            int screenY = y + (visualLine - startVisualLine) * LINE_HEIGHT;
            if (screenY >= y + height) break;

            // Draw line number only on first visual line of each logical line
            if (wrapOffset == 0) {
                gg.drawString(this.font, String.format("%3d", logicalLine + 1), x + 4, screenY, 0xFF888888);
            }

            // Draw wrapped content
            String wrappedPart = getWrappedPortion(logicalLine, wrapOffset, contentMaxWidth);

            if (!wrappedPart.isEmpty()) {
                // Draw selection background
                String[] wrapped = wrapLineByWords(lines[logicalLine], contentMaxWidth);
                int columnStart = 0;
                for (int w = 0; w < wrapOffset && w < wrapped.length; w++) {
                    columnStart += wrapped[w].length();
                }
                int columnEnd = columnStart + wrappedPart.length();

                // Draw selection for each character in this line
                for (int col = columnStart; col < columnEnd; col++) {
                    if (isPositionSelected(logicalLine, col)) {
                        String before = wrappedPart.substring(0, Math.max(0, col - columnStart));
                        String charStr = wrappedPart.substring(Math.max(0, col - columnStart), Math.min(wrappedPart.length(), col - columnStart + 1));
                        int charStartX = x + 32 + this.font.width(before);
                        int charWidth = this.font.width(charStr.isEmpty() ? " " : charStr);
                        gg.fill(charStartX, screenY, charStartX + charWidth, screenY + LINE_HEIGHT, 0xFF0066CC);
                    }
                }

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

            if (logicalLine == lines.length - 1) break;
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
        String statusText = "Ln " + (cursorLine + 1) + ", Col " + (cursorColumn + 1);
        if (selectionStartLine != -1 && selectionEndLine != -1) {
            statusText += " | Selection active";
        }
        gg.drawString(this.font, statusText, x, statusY, 0xFFAAAAAA);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle confirmation dialog clicks
        if (showingConfirmDialog && hasUnsavedChanges) {
            int dialogWidth = 320;
            int dialogHeight = 140;
            int dialogX = (this.width - dialogWidth) / 2;
            int dialogY = (this.height - dialogHeight) / 2;

            int buttonWidth = 80;
            int buttonHeight = 20;
            int buttonY = dialogY + dialogHeight - 32;
            int buttonSpacing = 12;
            int totalButtonWidth = (buttonWidth * 2) + buttonSpacing;
            int buttonsStartX = dialogX + (dialogWidth - totalButtonWidth) / 2;

            // Save button
            int saveButtonX = buttonsStartX;
            int saveButtonY = buttonY;
            if (mouseX >= saveButtonX && mouseX < saveButtonX + buttonWidth &&
                    mouseY >= saveButtonY && mouseY < saveButtonY + buttonHeight) {
                onSave();
                showingConfirmDialog = false;
                confirmAction = null;
                return true;
            }

            // Discard button
            int discardButtonX = buttonsStartX + buttonWidth + buttonSpacing;
            int discardButtonY = buttonY;
            if (mouseX >= discardButtonX && mouseX < discardButtonX + buttonWidth &&
                    mouseY >= discardButtonY && mouseY < discardButtonY + buttonHeight) {
                if (confirmAction != null) {
                    confirmAction.run();
                }
                return true;
            }

            return true; // Consume click on dialog
        }

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
            if (mouseX < scrollbarX && button == 0) { // Left mouse button only
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

                        // Check for multi-click
                        long currentTime = System.currentTimeMillis();
                        int clickDistance = Math.abs(lastClickX - (int) mouseX) + Math.abs(lastClickY - (int) mouseY);

                        if (currentTime - lastClickTime <= DOUBLE_CLICK_INTERVAL && clickDistance <= DOUBLE_CLICK_TOLERANCE) {
                            lastClickCount++;
                        } else {
                            lastClickCount = 1;
                        }

                        lastClickTime = currentTime;
                        lastClickX = (int) mouseX;
                        lastClickY = (int) mouseY;

                        // Handle multi-click
                        if (lastClickCount == 2) {
                            // Double-click: select word
                            selectWord();
                        } else if (lastClickCount >= 3) {
                            // Triple-click: select line
                            selectLine();
                            lastClickCount = 0; // Reset counter
                        } else {
                            // Single-click
                            // If shift is held, extend selection
                            if ((GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                                    GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) &&
                                    selectionStartLine != -1) {
                                // Extend selection
                                selectionEndLine = cursorLine;
                                selectionEndColumn = cursorColumn;
                            } else {
                                // Start new selection
                                selectionStartLine = cursorLine;
                                selectionStartColumn = cursorColumn;
                                selectionEndLine = cursorLine;
                                selectionEndColumn = cursorColumn;
                                isSelecting = true;
                            }
                        }

                        lastCursorBlink = System.currentTimeMillis();
                        cursorVisible = true;
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Don't process keys if confirmation dialog is showing
        if (showingConfirmDialog) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                showingConfirmDialog = false;
                confirmAction = null;
                return true;
            }
            return true; // Consume all other keys
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onCancel();
            return true;
        }

        // Shift pressed - select text with arrow keys
        if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                if (selectionEndLine == -1) {
                    selectionEndLine = cursorLine;
                    selectionEndColumn = cursorColumn;
                }
                if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                    previousWord();
                } else {
                    if (cursorColumn > 0) {
                        cursorColumn--;
                    } else if (cursorLine > 0) {
                        cursorLine--;
                        cursorColumn = lines[cursorLine].length();
                    }
                }
                selectionStartLine = cursorLine;
                selectionStartColumn = cursorColumn;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                if (selectionStartLine == -1) {
                    selectionStartLine = cursorLine;
                    selectionStartColumn = cursorColumn;
                }
                if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                    nextWord();
                } else {
                    if (cursorColumn < lines[cursorLine].length()) {
                        cursorColumn++;
                    } else if (cursorLine < lines.length - 1) {
                        cursorLine++;
                        cursorColumn = 0;
                    }
                }
                selectionEndLine = cursorLine;
                selectionEndColumn = cursorColumn;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                if (selectionStartLine == -1) {
                    selectionStartLine = cursorLine;
                    selectionStartColumn = cursorColumn;
                }
                int editorWidth = (this.width / 2) - DIVIDER_WIDTH / 2 - PADDING;
                int contentMaxWidth = editorWidth - 40;

                int currentVisualLine = getVisualLineIndex(cursorLine, contentMaxWidth);
                String[] wrapped = wrapLineByWords(lines[cursorLine], contentMaxWidth);

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
                selectionEndLine = cursorLine;
                selectionEndColumn = cursorColumn;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                if (selectionStartLine == -1) {
                    selectionStartLine = cursorLine;
                    selectionStartColumn = cursorColumn;
                }
                int editorWidth = (this.width / 2) - DIVIDER_WIDTH / 2 - PADDING;
                int contentMaxWidth = editorWidth - 40;

                int currentVisualLine = getVisualLineIndex(cursorLine, contentMaxWidth);
                String[] wrapped = wrapLineByWords(lines[cursorLine], contentMaxWidth);

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
                selectionEndLine = cursorLine;
                selectionEndColumn = cursorColumn;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_HOME) {
                if (selectionStartLine == -1) {
                    selectionStartLine = cursorLine;
                    selectionStartColumn = cursorColumn;
                }
                cursorColumn = 0;
                selectionEndLine = cursorLine;
                selectionEndColumn = cursorColumn;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_END) {
                if (selectionStartLine == -1) {
                    selectionStartLine = cursorLine;
                    selectionStartColumn = cursorColumn;
                }
                cursorColumn = lines[cursorLine].length();
                selectionEndLine = cursorLine;
                selectionEndColumn = cursorColumn;
                return true;
            }
        }

        // Ctrl pressed
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            if (keyCode == GLFW.GLFW_KEY_S) {
                onSave();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_Y) {
                // Undo
                undo();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_Z) {
                // Redo
                redo();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_C) {
                // Copy selected text
                String selectedText = getSelectedText();
                if (!selectedText.isEmpty()) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(selectedText);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_X) {
                // Cut selected text
                String selectedText = getSelectedText();
                if (!selectedText.isEmpty()) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(selectedText);
                    saveUndoState();
                    deleteSelection();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_V) {
                // Paste with newline support
                String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                if (!clipboard.isEmpty()) {
                    saveUndoState();
                    // If text is selected, delete it first
                    if (selectionStartLine != -1 && selectionEndLine != -1) {
                        deleteSelection();
                    }

                    // Insert clipboard content
                    String[] clipboardLines = clipboard.split("\\r?\\n", -1);

                    if (clipboardLines.length == 1) {
                        // Single line paste
                        String line = lines[cursorLine];
                        lines[cursorLine] = line.substring(0, cursorColumn) + clipboardLines[0] + line.substring(cursorColumn);
                        cursorColumn += clipboardLines[0].length();
                    } else {
                        // Multi-line paste
                        String line = lines[cursorLine];
                        String beforeCursor = line.substring(0, cursorColumn);
                        String afterCursor = line.substring(cursorColumn);

                        // First line
                        lines[cursorLine] = beforeCursor + clipboardLines[0];

                        // Middle lines
                        for (int i = 1; i < clipboardLines.length - 1; i++) {
                            lines = insertIndex(lines, cursorLine + i, clipboardLines[i]);
                        }

                        // Last line
                        String lastLine = clipboardLines[clipboardLines.length - 1] + afterCursor;
                        lines = insertIndex(lines, cursorLine + clipboardLines.length - 1, lastLine);

                        cursorLine += clipboardLines.length - 1;
                        cursorColumn = clipboardLines[clipboardLines.length - 1].length();
                    }

                    hasUnsavedChanges = true;
                    clearSelection();
                    updatePreview();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_A) {
                // Select all
                selectionStartLine = 0;
                selectionStartColumn = 0;
                selectionEndLine = lines.length - 1;
                selectionEndColumn = lines[lines.length - 1].length();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                // Move to previous word
                clearSelection();
                previousWord();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                // Move to next word
                clearSelection();
                nextWord();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                // Delete word backward
                if (cursorColumn > 0) {
                    saveUndoState();
                    String line = lines[cursorLine];
                    var prevWhitespace = lines[cursorLine].substring(0, cursorColumn).lastIndexOf(' ');
                    if (prevWhitespace == -1) prevWhitespace = 0;
                    var newLine = line.substring(0, prevWhitespace);
                    if (cursorColumn < lines[cursorLine].length()) newLine += line.substring(cursorColumn);
                    lines[cursorLine] = newLine;
                    hasUnsavedChanges = true;
                    cursorColumn -= cursorColumn - prevWhitespace;
                    updatePreview();
                } else if (cursorLine > 0) {
                    saveUndoState();
                    lines[cursorLine] = lines[cursorLine - 1] + lines[cursorLine];
                    lines = removeIndex(lines, cursorLine);
                    cursorLine -= 1;
                    cursorColumn = lines[cursorLine].length();
                    hasUnsavedChanges = true;
                    updatePreview();
                }
                return true;
            }
        }

        // Get content max width for wrapped line navigation
        int editorWidth = (this.width / 2) - DIVIDER_WIDTH / 2 - PADDING;
        int contentMaxWidth = editorWidth - 40;

        // Arrow keys - navigate by visual lines
        if (keyCode == GLFW.GLFW_KEY_UP) {
            clearSelection();
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
            clearSelection();
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
            if (selectionStartLine != -1 && selectionEndLine != -1) {
                normalizeSelection();
                cursorLine = selectionStartLine;
                cursorColumn = selectionStartColumn;
                clearSelection();
            } else if (cursorColumn > 0) {
                cursorColumn--;
            } else if (cursorLine > 0) {
                cursorLine--;
                cursorColumn = lines[cursorLine].length();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (selectionStartLine != -1 && selectionEndLine != -1) {
                normalizeSelection();
                cursorLine = selectionEndLine;
                cursorColumn = selectionEndColumn;
                clearSelection();
            } else if (cursorColumn < lines[cursorLine].length()) {
                cursorColumn++;
            } else if (cursorLine < lines.length - 1) {
                cursorLine++;
                cursorColumn = 0;
            }
            return true;
        }

        // Home / End
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            clearSelection();
            cursorColumn = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            clearSelection();
            cursorColumn = lines[cursorLine].length();
            return true;
        }

        // Page Up / Down - navigate by visual lines
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            clearSelection();
            int currentVisualLine = getVisualLineIndex(cursorLine, contentMaxWidth);
            int editorHeight = (this.height - PADDING * 3 - BUTTON_HEIGHT - 24);
            int visibleVisualLines = editorHeight / LINE_HEIGHT;
            int visualLine = Math.max(0, currentVisualLine - visibleVisualLines);
            int[] logicalInfo = visualLineToLogical(visualLine, contentMaxWidth);
            cursorLine = logicalInfo[0];
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            clearSelection();
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
            // If text is selected, delete the selection
            if (selectionStartLine != -1 && selectionEndLine != -1) {
                saveUndoState();
                deleteSelection();
            } else if (cursorColumn > 0) {
                saveUndoState();
                String line = lines[cursorLine];
                lines[cursorLine] = line.substring(0, cursorColumn - 1) + line.substring(cursorColumn);
                cursorColumn--;
                hasUnsavedChanges = true;
                updatePreview();
            } else if (cursorLine > 0) {
                saveUndoState();
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
            // If text is selected, delete the selection
            if (selectionStartLine != -1 && selectionEndLine != -1) {
                saveUndoState();
                deleteSelection();
            } else if (cursorColumn < lines[cursorLine].length()) {
                saveUndoState();
                String line = lines[cursorLine];
                lines[cursorLine] = line.substring(0, cursorColumn) + line.substring(cursorColumn + 1);
                hasUnsavedChanges = true;
                updatePreview();
            } else if (cursorLine < lines.length - 1) {
                saveUndoState();
                lines[cursorLine] = lines[cursorLine] + lines[cursorLine + 1];
                lines = removeIndex(lines, cursorLine + 1);
                hasUnsavedChanges = true;
                updatePreview();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            saveUndoState();
            // If text is selected, delete it first
            if (selectionStartLine != -1 && selectionEndLine != -1) {
                deleteSelection();
            }

            String line = lines[cursorLine];
            String before = line.substring(0, cursorColumn);
            String after = line.substring(cursorColumn);
            lines[cursorLine] = before;
            lines = insertIndex(lines, cursorLine + 1, after);
            cursorLine++;
            cursorColumn = 0;
            hasUnsavedChanges = true;
            clearSelection();
            updatePreview();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_TAB) {
            saveUndoState();
            // If text is selected, delete it first
            if (selectionStartLine != -1 && selectionEndLine != -1) {
                deleteSelection();
            }

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
            saveUndoState();
            // If text is selected, delete it first
            if (selectionStartLine != -1 && selectionEndLine != -1) {
                deleteSelection();
            }

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

        // Handle text selection with mouse drag
        if (isSelecting && button == 0) {
            int topY = PADDING + 24;
            int bottomY = this.height - PADDING - BUTTON_HEIGHT;
            int centerX = PADDING + (this.width - 2 * PADDING) / 2;

            int editorX = PADDING;
            int editorHeight = bottomY - topY - 8;
            int editorWidth = centerX - DIVIDER_WIDTH / 2 - PADDING;
            int contentMaxWidth = editorWidth - 40;

            if (mouseX >= editorX && mouseX < centerX && mouseY >= topY && mouseY < bottomY) {
                int relativeY = (int) (mouseY - topY);
                int visibleVisualLines = editorHeight / LINE_HEIGHT;
                int visualLine = relativeY / LINE_HEIGHT + scrollY;

                if (visualLine >= 0) {
                    int[] logicalInfo = visualLineToLogical(visualLine, contentMaxWidth);
                    int logicalLine = logicalInfo[0];
                    int wrapOffset = logicalInfo[1];

                    if (logicalLine < lines.length) {
                        String wrappedPart = getWrappedPortion(logicalLine, wrapOffset, contentMaxWidth);
                        String[] wrapped = wrapLineByWords(lines[logicalLine], contentMaxWidth);

                        int columnStart = 0;
                        for (int w = 0; w < wrapOffset && w < wrapped.length; w++) {
                            columnStart += wrapped[w].length();
                        }

                        double relativeX = mouseX - editorX - 32;
                        if (relativeX < 0) relativeX = 0;

                        int estimatedOffset = 0;
                        for (int i = 0; i < wrappedPart.length(); i++) {
                            int charWidth = this.font.width(wrappedPart.substring(0, i + 1));
                            if (charWidth > relativeX) break;
                            estimatedOffset = i + 1;
                        }

                        int newColumn = columnStart + estimatedOffset;
                        newColumn = Math.max(0, Math.min(newColumn, lines[logicalLine].length()));

                        // Update selection end
                        selectionEndLine = logicalLine;
                        selectionEndColumn = newColumn;
                        normalizeSelection();
                    }
                }
            }
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void onCancel() {
        if (hasUnsavedChanges) {
            // Show confirmation dialog
            showingConfirmDialog = true;
            confirmAction = () -> {
                showingConfirmDialog = false;
                Minecraft.getInstance().setScreen(previousScreen);
            };
        } else {
            Minecraft.getInstance().setScreen(previousScreen);
        }
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

    @Override
    public void onClose() {
        if (hasUnsavedChanges) {
            // Show confirmation dialog
            showingConfirmDialog = true;
            confirmAction = () -> {
                showingConfirmDialog = false;
                super.onClose();
            };
        } else {
            super.onClose();
        }
    }

    /**
     * Normalizes selection so that start is before end
     */
    private void normalizeSelection() {
        if (selectionStartLine == -1 || selectionEndLine == -1) return;

        if (selectionStartLine > selectionEndLine ||
                (selectionStartLine == selectionEndLine && selectionStartColumn > selectionEndColumn)) {
            // Swap
            int tempLine = selectionStartLine;
            int tempCol = selectionStartColumn;
            selectionStartLine = selectionEndLine;
            selectionStartColumn = selectionEndColumn;
            selectionEndLine = tempLine;
            selectionEndColumn = tempCol;
        }
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

    /**
     * Checks if a position is within the selection
     */
    private boolean isPositionSelected(int line, int column) {
        if (selectionStartLine == -1 || selectionEndLine == -1) return false;

        normalizeSelection();

        if (line < selectionStartLine || line > selectionEndLine) return false;
        if (line == selectionStartLine && line == selectionEndLine) {
            return column >= selectionStartColumn && column < selectionEndColumn;
        }
        if (line == selectionStartLine) {
            return column >= selectionStartColumn;
        }
        if (line == selectionEndLine) {
            return column < selectionEndColumn;
        }
        return true;
    }

    /**
     * Gets the selected text
     */
    private String getSelectedText() {
        if (selectionStartLine == -1 || selectionEndLine == -1) return "";

        normalizeSelection();

        if (selectionStartLine == selectionEndLine) {
            return lines[selectionStartLine].substring(selectionStartColumn, selectionEndColumn);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(lines[selectionStartLine].substring(selectionStartColumn));
        for (int i = selectionStartLine + 1; i < selectionEndLine; i++) {
            sb.append("\n").append(lines[i]);
        }
        sb.append("\n").append(lines[selectionEndLine], 0, selectionEndColumn);
        return sb.toString();
    }

    /**
     * Clears the selection
     */
    private void clearSelection() {
        selectionStartLine = -1;
        selectionStartColumn = -1;
        selectionEndLine = -1;
        selectionEndColumn = -1;
        isSelecting = false;
    }

    /**
     * Deletes the selected text
     */
    private void deleteSelection() {
        if (selectionStartLine == -1 || selectionEndLine == -1) return;

        normalizeSelection();

        if (selectionStartLine == selectionEndLine) {
            String line = lines[selectionStartLine];
            lines[selectionStartLine] = line.substring(0, selectionStartColumn) + line.substring(selectionEndColumn);
            cursorLine = selectionStartLine;
            cursorColumn = selectionStartColumn;
        } else {
            String startLine = lines[selectionStartLine].substring(0, selectionStartColumn);
            String endLine = lines[selectionEndLine].substring(selectionEndColumn);
            lines[selectionStartLine] = startLine + endLine;

            // Remove lines between start and end
            for (int i = selectionEndLine; i > selectionStartLine; i--) {
                lines = removeIndex(lines, i);
            }

            cursorLine = selectionStartLine;
            cursorColumn = selectionStartColumn;
        }

        clearSelection();
        hasUnsavedChanges = true;
        updatePreview();
    }

    /**
     * Saves current editor state to undo stack
     */
    private void saveUndoState() {
        undoStack.push(new EditorState(lines, cursorLine, cursorColumn));
        if (undoStack.size() > MAX_UNDO_HISTORY) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    /**
     * Performs undo operation
     */
    private void undo() {
        if (undoStack.isEmpty()) return;

        // Save current state to redo stack
        redoStack.push(new EditorState(lines, cursorLine, cursorColumn));

        // Restore previous state
        EditorState state = undoStack.pop();
        lines = state.lines;
        cursorLine = state.cursorLine;
        cursorColumn = state.cursorColumn;
        clearSelection();
        updatePreview();
        hasUnsavedChanges = true;
    }

    /**
     * Performs redo operation
     */
    private void redo() {
        if (redoStack.isEmpty()) return;

        // Save current state to undo stack
        undoStack.push(new EditorState(lines, cursorLine, cursorColumn));

        // Restore next state
        EditorState state = redoStack.pop();
        lines = state.lines;
        cursorLine = state.cursorLine;
        cursorColumn = state.cursorColumn;
        clearSelection();
        updatePreview();
        hasUnsavedChanges = true;
    }

    /**
     * Checks if a character is a word separator
     */
    private boolean isWordSeparator(char c) {
        return !Character.isLetterOrDigit(c) && c != '_';
    }

    /**
     * Moves cursor to the next word start
     */
    private void nextWord() {
        String line = lines[cursorLine];

        // Skip current word
        while (cursorColumn < line.length() && !isWordSeparator(line.charAt(cursorColumn))) {
            cursorColumn++;
        }

        // Skip separators
        while (cursorColumn < line.length() && isWordSeparator(line.charAt(cursorColumn))) {
            cursorColumn++;
        }

        // If at end of line, go to next line
        if (cursorColumn >= line.length() && cursorLine < lines.length - 1) {
            cursorLine++;
            cursorColumn = 0;
            nextWord(); // Continue on next line
        }
    }

    /**
     * Moves cursor to the previous word start
     */
    private void previousWord() {
        if (cursorColumn == 0) {
            if (cursorLine > 0) {
                cursorLine--;
                cursorColumn = lines[cursorLine].length();
                previousWord(); // Continue on previous line
            }
            return;
        }

        cursorColumn--;
        String line = lines[cursorLine];

        // Skip separators
        while (cursorColumn > 0 && isWordSeparator(line.charAt(cursorColumn))) {
            cursorColumn--;
        }

        // Skip word
        while (cursorColumn > 0 && !isWordSeparator(line.charAt(cursorColumn - 1))) {
            cursorColumn--;
        }
    }

    /**
     * Selects the word at the current cursor position
     */
    private void selectWord() {
        String line = lines[cursorLine];
        if (line.isEmpty()) {
            selectionStartLine = cursorLine;
            selectionStartColumn = 0;
            selectionEndLine = cursorLine;
            selectionEndColumn = 0;
            return;
        }

        // Find word boundaries
        int start = cursorColumn;
        int end = cursorColumn;

        // Move start to beginning of word
        while (start > 0 && !isWordSeparator(line.charAt(start - 1))) {
            start--;
        }

        // Move end to end of word
        while (end < line.length() && !isWordSeparator(line.charAt(end))) {
            end++;
        }

        selectionStartLine = cursorLine;
        selectionStartColumn = start;
        selectionEndLine = cursorLine;
        selectionEndColumn = end;
    }

    /**
     * Selects the entire line at the current cursor position
     */
    private void selectLine() {
        selectionStartLine = cursorLine;
        selectionStartColumn = 0;
        selectionEndLine = cursorLine;
        selectionEndColumn = lines[cursorLine].length();
    }

    // Undo/Redo state storage
    private static class EditorState {
        String[] lines;
        int cursorLine;
        int cursorColumn;

        EditorState(String[] lines, int cursorLine, int cursorColumn) {
            this.lines = lines.clone();
            this.cursorLine = cursorLine;
            this.cursorColumn = cursorColumn;
        }
    }

    // ...existing code...
}





