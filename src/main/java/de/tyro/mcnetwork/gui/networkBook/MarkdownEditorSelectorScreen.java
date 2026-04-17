package de.tyro.mcnetwork.gui.networkBook;

import de.tyro.mcnetwork.networkBook.data.SubTopic;
import de.tyro.mcnetwork.networkBook.data.Topic;
import de.tyro.mcnetwork.networkBook.data.TopicManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Selection screen for choosing which subtopic to edit.
 * Displays all available topics and their subtopics.
 */
public class MarkdownEditorSelectorScreen extends Screen {

    private static final int ITEM_HEIGHT = 24;
    private static final int PADDING = 16;
    private final Screen previousScreen;
    private final List<Topic> topics;
    private final List<EditableItem> editableItems = new ArrayList<>();
    private int scrollY = 0;
    private int selectedIndex = -1;

    public MarkdownEditorSelectorScreen(Screen previousScreen) {
        super(Component.literal("Select Chapter to Edit"));
        this.previousScreen = previousScreen;
        this.topics = TopicManager.getInstance().getTopics();

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

        // Back button
        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
                .pos(PADDING, this.height - PADDING - 20)
                .size(80, 20)
                .build());

        // Edit button
        this.addRenderableWidget(Button.builder(Component.literal("Edit"), button -> onEdit())
                .pos(PADDING + 90, this.height - PADDING - 20)
                .size(60, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        super.render(gg, mouseX, mouseY, partialTicks);

        // Draw title
        gg.drawString(this.font, "Markdown Editor - Select Chapter", PADDING, PADDING, 0xFFFFFF);

        // Draw list background
        int listX = PADDING;
        int listY = PADDING + 24;
        int listWidth = this.width - 2 * PADDING;
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

    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollY = Math.max(0, (int) (this.scrollY - scrollY * 3));
        int maxScroll = Math.max(0, editableItems.size() - (this.height - 80) / ITEM_HEIGHT);
        this.scrollY = Math.min(this.scrollY, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = PADDING;
        int listY = PADDING + 24;
        int listWidth = this.width - 2 * PADDING;
        int listHeight = this.height - PADDING * 3 - 40;

        if (mouseX >= listX && mouseX < listX + listWidth &&
                mouseY >= listY && mouseY < listY + listHeight) {

            int itemIndex = (int) ((mouseY - listY) / ITEM_HEIGHT) + scrollY;
            if (itemIndex < editableItems.size()) {
                selectedIndex = itemIndex;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_UP) {
            if (selectedIndex > 0) selectedIndex--;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            if (selectedIndex < editableItems.size() - 1) selectedIndex++;
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

    private void onEdit() {
        if (selectedIndex >= 0 && selectedIndex < editableItems.size()) {
            EditableItem item = editableItems.get(selectedIndex);
            if (item.isSubtopic) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                Minecraft.getInstance().setScreen(new MarkdownEditorScreen(this, item.subtopic, item.topic));
            }
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(previousScreen);
    }

    @Override
    @SuppressWarnings("override")
    public boolean isPauseScreen() {
        return true;
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
}


