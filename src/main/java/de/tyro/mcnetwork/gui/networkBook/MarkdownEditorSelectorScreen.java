package de.tyro.mcnetwork.gui.networkBook;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.networkBook.data.MarkdownFileManager;
import de.tyro.mcnetwork.networkBook.data.SubTopic;
import de.tyro.mcnetwork.networkBook.data.Topic;
import de.tyro.mcnetwork.networkBook.data.TopicManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Selection screen for choosing which subtopic to edit.
 * Displays all available topics and their subtopics.
 */
public class MarkdownEditorSelectorScreen extends Screen {

    private static final int ITEM_HEIGHT = 24;
    private static final int PADDING = 16;
    private static final int DIVIDER_WIDTH = 2;
    private static final int INPUT_HEIGHT = 24;

    private final Screen previousScreen;
    private final List<Topic> topics;
    private final List<EditableItem> editableItems = new ArrayList<>();
    private int scrollY = 0;
    private int selectedIndex = -1;

    // Right panel - Settings editor fields
    private EditBox titleInput;
    private EditBox posXInput;
    private EditBox posYInput;
    private EditBox iconInput;
    private EditBox preInput;
    private boolean hasSettingsChanged = false;

    // Mini topic visualization
    private final DraggablePlane miniPlane;
    private Topic currentMiniTopic;

    public MarkdownEditorSelectorScreen(Screen previousScreen) {
        super(Component.literal("Select Chapter to Edit"));
        this.previousScreen = previousScreen;
        this.topics = TopicManager.getInstance().getTopics();

        // Initialize mini plane for topic visualization
        this.miniPlane = new DraggablePlane(null, 0, 0, 200, 150);

        // Build list of editable items (topics and subtopics)
        buildEditableList();
    }

    private void buildEditableList() {
        editableItems.clear();
        for (Topic topic : topics) {
            editableItems.add(new EditableItem(topic, null, 0));
            for (SubTopic subtopic : topic.getSubtopics()) {
                editableItems.add(new EditableItem(topic, subtopic, 1));
            }
        }
    }

    @Override
    public void init() {
        super.init();

        int centerX = this.width / 2;

        // Left panel buttons
        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
                .pos(PADDING, this.height - PADDING - 20)
                .size(80, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Edit Markdown"), button -> onEdit())
                .pos(PADDING + 90, this.height - PADDING - 20)
                .size(110, 20)
                .build());

        // Right panel settings inputs
        int inputStartY = PADDING + 24;
        int inputX = centerX + PADDING + 80;
        int inputWidth = this.width - inputX - PADDING;

        // Title input
        this.titleInput = new EditBox(this.font, inputX, inputStartY, inputWidth, INPUT_HEIGHT, Component.literal("Title"));
        this.addRenderableWidget(titleInput);

        // PosX input
        inputStartY += INPUT_HEIGHT + 4;
        this.posXInput = new EditBox(this.font, inputX, inputStartY, inputWidth / 2 - 2, INPUT_HEIGHT, Component.literal("Position X"));
        posXInput.setMaxLength(5);
        this.addRenderableWidget(posXInput);

        // PosY input
        this.posYInput = new EditBox(this.font, inputX + inputWidth / 2 + 2, inputStartY, inputWidth / 2 - 2, INPUT_HEIGHT, Component.literal("Position Y"));
        posYInput.setMaxLength(5);
        this.addRenderableWidget(posYInput);

        // Icon input
        inputStartY += INPUT_HEIGHT + 4;
        this.iconInput = new EditBox(this.font, inputX, inputStartY, inputWidth, INPUT_HEIGHT, Component.literal("Icon"));
        this.addRenderableWidget(iconInput);

        // Prerequisites input
        inputStartY += INPUT_HEIGHT + 4;
        this.preInput = new EditBox(this.font, inputX, inputStartY, inputWidth, INPUT_HEIGHT, Component.literal("Prerequisites"));
        this.preInput.setMaxLength(500);
        this.addRenderableWidget(preInput);

        // Save Settings button
        this.addRenderableWidget(Button.builder(Component.literal("Save Settings"), button -> onSaveSettings())
                .pos(centerX + PADDING, this.height - PADDING - 20)
                .size(100, 20)
                .build());

        setSettingsVisible(false);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {

        // Draw title
        gg.drawString(this.font, "Markdown Editor - Select Chapter", PADDING, PADDING, 0xFFFFFF);

        int centerX = this.width / 2;
        int miniPlaneSize = 200;
        int miniPlaneX = this.width - PADDING - miniPlaneSize;
        int miniPlaneY = this.height - PADDING - 150;

        // LEFT PANEL - LIST
        int listX = PADDING;
        int listY = PADDING + 24;
        int listWidth = centerX - PADDING - DIVIDER_WIDTH / 2;
        int listHeight = this.height - PADDING * 3 - 40;

        gg.fill(listX, listY, listX + listWidth, listY + listHeight, 0xFF222228);
        gg.fill(listX - 1, listY - 1, listX + listWidth + 1, listY + listHeight + 1, 0xFF666666);

        // Draw items
        gg.enableScissor(listX, listY, listX + listWidth, listY + listHeight);

        int visibleItems = listHeight / ITEM_HEIGHT;
        int startIndex = Math.max(0, scrollY);
        int endIndex = Math.min(editableItems.size(), startIndex + visibleItems + 2);

        for (int i = startIndex; i < endIndex; i++) {
            int itemY = listY + (i - startIndex) * ITEM_HEIGHT;
            EditableItem item = editableItems.get(i);

            // Highlight selected item
            if (i == selectedIndex) {
                gg.fill(listX, itemY, listX + listWidth, itemY + ITEM_HEIGHT, 0xFF444466);
            }

            // Draw item text
            String displayText = item.getDisplayText();
            int textIndent = item.isSubtopic ? 24 : 8;
            int textColor = item.isSubtopic ? 0xFFCCCCCC : 0xFFFFFFFF;
            gg.drawString(this.font, displayText, listX + textIndent, itemY + 6, textColor);
        }

        gg.disableScissor();

        // DIVIDER
        gg.fill(centerX - DIVIDER_WIDTH / 2, listY, centerX + DIVIDER_WIDTH / 2, listY + listHeight, 0xFF666666);

        // RIGHT PANEL - SETTINGS (adjusted for mini plane)
        int settingsHeight = miniPlaneY - listY - PADDING;
        if (selectedIndex >= 0 && selectedIndex < editableItems.size()) {
            EditableItem selectedItem = editableItems.get(selectedIndex);
            if (selectedItem.isSubtopic) {
                renderSettingsPanel(gg, centerX, listY, settingsHeight);
            } else {
                // Topic selected - show message
                int panelX = centerX + PADDING;
                gg.drawString(this.font, "Select a subtopic to edit settings", panelX, listY + 20, 0xFF888888);
            }
        } else {
            // Nothing selected - show message
            int panelX = centerX + PADDING;
            gg.drawString(this.font, "Select a subtopic to edit settings", panelX, listY + 20, 0xFF888888);
        }

        // MINI TOPIC VISUALIZATION - BOTTOM RIGHT
        renderMiniTopicVisualization(gg, miniPlaneX, miniPlaneY, miniPlaneSize, 150, mouseX, mouseY, partialTicks);

        // Draw unsaved indicator
        if (hasSettingsChanged) {
            gg.drawString(this.font, "● Unsaved Changes", this.width - PADDING - 120, PADDING, 0xFFFF6600);
        }

        for (Renderable renderable : this.renderables) {
            renderable.render(gg, mouseX, mouseY, partialTicks);
        }
    }

    private void renderSettingsPanel(GuiGraphics gg, int panelStartX, int panelStartY, int panelHeight) {
        int panelX = panelStartX + PADDING;

        // Background
        gg.fill(panelX - PADDING, panelStartY, this.width, panelStartY + panelHeight, 0xFF1a1a1e);

        int startY = panelStartY + 8;

        // Title label
        gg.drawString(this.font, "Title:", panelX, startY, 0xFFCCCCCC);
        startY += INPUT_HEIGHT;

        // Position labels
        gg.drawString(this.font, "Position:", panelX, startY, 0xFFCCCCCC);
        startY += INPUT_HEIGHT;

        // Icon label
        gg.drawString(this.font, "Icon:", panelX, startY, 0xFFCCCCCC);
        startY += INPUT_HEIGHT;

        // Prerequisites label
        gg.drawString(this.font, "Prerequisites:", panelX, startY, 0xFFCCCCCC);
        gg.drawString(this.font, "(comma-sep.)", panelX, startY + 12, 0xFF888888);
    }

    private void renderMiniTopicVisualization(GuiGraphics gg, int x, int y, int width, int height, int mouseX, int mouseY, float partialTicks) {
        // Background
        gg.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF666666);
        gg.fill(x, y, x + width, y + height, 0xFF111216);

        // Title
        String title = currentMiniTopic != null ? currentMiniTopic.getTitle() : "No Topic Selected";
        gg.drawString(this.font, title, x + 8, y + 4, 0xFFFFFF);

        // Render the mini plane if we have a topic
        if (currentMiniTopic != null && miniPlane != null) {
            gg.enableScissor(x, y + 20, x + width, y + height);

            gg.pose().pushPose();
            gg.pose().translate(x, y + 20, 0);

            // Create a temporary RenderUtil for the mini plane
            RenderUtil renderer = new RenderUtil(new PoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), 1, 0);
            renderer.setPose(gg.pose());

            // Update mini plane position and render
            miniPlane.render(gg, mouseX - x, mouseY - (y + 20), partialTicks);

            gg.pose().popPose();
            gg.disableScissor();
        } else {
            // No topic selected message
            gg.drawString(this.font, "Select a topic to", x + 8, y + 40, 0xFF888888);
            gg.drawString(this.font, "see visualization", x + 8, y + 52, 0xFF888888);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int centerX = this.width / 2;

        // Only scroll list if mouse is on left side
        if (mouseX < centerX) {
            this.scrollY = Math.max(0, (int) (this.scrollY - scrollY * 3));
            int maxScroll = Math.max(0, editableItems.size() - (this.height - 80) / ITEM_HEIGHT);
            this.scrollY = Math.min(this.scrollY, maxScroll);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int listX = PADDING;
        int listY = PADDING + 24;
        int listWidth = centerX - PADDING - DIVIDER_WIDTH / 2;
        int listHeight = this.height - PADDING * 3 - 40;

        // Click on left panel - list selection
        if (mouseX >= listX && mouseX < listX + listWidth &&
                mouseY >= listY && mouseY < listY + listHeight) {

            int itemIndex = (int) ((mouseY - listY) / ITEM_HEIGHT) + scrollY;
            if (itemIndex < editableItems.size()) {
                selectedIndex = itemIndex;
                loadSettingsForSelected();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void loadSettingsForSelected() {
        if (selectedIndex < 0 || selectedIndex >= editableItems.size()) return;

        EditableItem item = editableItems.get(selectedIndex);

        // Update mini topic visualization
        if (item.topic != currentMiniTopic) {
            currentMiniTopic = item.topic;
            if (miniPlane != null && currentMiniTopic != null) {
                miniPlane.setSubtopics(currentMiniTopic.getSubtopics());
            }
        }

        if (!item.isSubtopic) {
            setSettingsVisible(false);
            return;
        }

        Map<String, Object> data = MarkdownFileManager.readYamlData(item.subtopic.getLocation());
        if (data != null) {
            titleInput.setValue(data.getOrDefault("title", "").toString());

            int posX = toInt(data.get("posX"), 0);
            int posY = toInt(data.get("posY"), 0);
            posXInput.setValue(String.valueOf(posX));
            posYInput.setValue(String.valueOf(posY));

            iconInput.setValue(data.getOrDefault("icon", "").toString());

            // Load prerequisites
            Object preObj = data.get("pre");
            if (preObj instanceof List<?> preList) {
                preInput.setValue(String.join(", ", preList.stream().map(Object::toString).toList()));
            } else {
                preInput.setValue(preObj != null ? preObj.toString() : "");
            }

            hasSettingsChanged = false;
        }

        setSettingsVisible(true);
    }

    private void setSettingsVisible(boolean b) {
        titleInput.setVisible(b);
        posXInput.setVisible(b);
        posYInput.setVisible(b);
        iconInput.setVisible(b);
        preInput.setVisible(b);
    }

    private int toInt(Object obj, int defaultValue) {
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_UP) {
            if (selectedIndex > 0) {
                selectedIndex--;
                loadSettingsForSelected();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            if (selectedIndex < editableItems.size() - 1) {
                selectedIndex++;
                loadSettingsForSelected();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            onEdit();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        // Check if values have changed
        if (selectedIndex >= 0 && selectedIndex < editableItems.size()) {
            EditableItem item = editableItems.get(selectedIndex);
            if (item.isSubtopic) {
                Map<String, Object> data = MarkdownFileManager.readYamlData(item.subtopic.getLocation());
                if (data != null) {
                    hasSettingsChanged = !titleInput.getValue().equals(data.getOrDefault("title", "").toString()) ||
                            !posXInput.getValue().equals(String.valueOf(toInt(data.get("posX"), 0))) ||
                            !posYInput.getValue().equals(String.valueOf(toInt(data.get("posY"), 0))) ||
                            !iconInput.getValue().equals(data.getOrDefault("icon", "").toString());
                }
            }
        }
    }

    private void onEdit() {
        if (selectedIndex >= 0 && selectedIndex < editableItems.size()) {
            EditableItem item = editableItems.get(selectedIndex);
            if (item.isSubtopic) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                Minecraft.getInstance().setScreen(new MarkdownEditorScreen(this, item.subtopic, item.topic));
            }
        }
    }

    private void onSaveSettings() {
        if (selectedIndex < 0 || selectedIndex >= editableItems.size()) return;

        EditableItem item = editableItems.get(selectedIndex);
        if (!item.isSubtopic) return;

        // Load current YAML data
        Map<String, Object> data = MarkdownFileManager.readYamlData(item.subtopic.getLocation());
        if (data == null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_TAKE_RESULT, 1.0f));
            return;
        }

        // Update fields
        data.put("title", titleInput.getValue());

        try {
            int posX = Integer.parseInt(posXInput.getValue());
            int posY = Integer.parseInt(posYInput.getValue());
            data.put("posX", posX);
            data.put("posY", posY);
            item.subtopic.setPosition(posX, posY);
        } catch (NumberFormatException e) {
            // Keep original values if parsing fails
        }

        String icon = iconInput.getValue();
        data.put("icon", icon);

        // Handle prerequisites
        String preStr = preInput.getValue().trim();
        if (preStr.isEmpty()) {
            data.remove("pre");
        } else {
            List<String> preList = new ArrayList<>();
            for (String pre : preStr.split(",")) {
                String trimmed = pre.trim();
                if (!trimmed.isEmpty()) {
                    preList.add(trimmed);
                }
            }
            data.put("pre", preList);
        }

        // Save to file
        boolean success = MarkdownFileManager.writeYamlData(item.subtopic.getLocation(), data);
        if (success) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_TAKE_RESULT, 1.0f));
            hasSettingsChanged = false;

            // Update the subtopic title and reload
            item.subtopic.setTitle(titleInput.getValue());
            TopicManager.getInstance().reloadTopic(item.topic);

            // Rebuild the list to show updated titles
            buildEditableList();

            selectedIndex = -1;
            setSettingsVisible(false);
        } else {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_TAKE_RESULT, 1.0f));
        }

    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(previousScreen);
    }

    /**
     * Helper class representing an editable item (topic or subtopic).
     */
    private static class EditableItem {
        final Topic topic;
        final SubTopic subtopic;
        final int depth;
        final boolean isSubtopic;

        EditableItem(Topic topic, SubTopic subtopic, int depth) {
            this.topic = topic;
            this.subtopic = subtopic;
            this.depth = depth;
            this.isSubtopic = subtopic != null;
        }

        String getDisplayText() {
            if (isSubtopic) {
                return "▪ " + subtopic.getTitle();
            } else {
                return "■ " + topic.getTitle();
            }
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Handle mini plane dragging
        int miniPlaneSize = 200;
        int miniPlaneX = this.width - PADDING - miniPlaneSize;
        int miniPlaneY = this.height - PADDING - 150;

        if (mouseX >= miniPlaneX && mouseX < miniPlaneX + miniPlaneSize &&
                mouseY >= miniPlaneY && mouseY < miniPlaneY + 150) {
            if (miniPlane != null) {
                return miniPlane.mouseDragged(mouseX - miniPlaneX, mouseY - (miniPlaneY + 20), button, dragX, dragY);
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}
