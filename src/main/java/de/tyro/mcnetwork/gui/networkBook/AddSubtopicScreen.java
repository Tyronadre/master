package de.tyro.mcnetwork.gui.networkBook;

import de.tyro.mcnetwork.networkBook.data.MarkdownFileManager;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Screen for adding a new subtopic to a topic.
 */
public class AddSubtopicScreen extends Screen {
    private static final int PADDING = 16;
    private static final int INPUT_HEIGHT = 20;

    private final Screen previousScreen;
    private final Topic topic;
    int panelWidth = 300;
    int panelHeight = 250;
    private EditBox subtopicNameInput;
    private EditBox posXInput;
    private EditBox posYInput;
    private EditBox iconInput;

    public AddSubtopicScreen(Screen previousScreen, Topic topic) {
        super(Component.literal("Add Subtopic"));
        this.previousScreen = previousScreen;
        this.topic = topic;
    }

    @Override
    public void init() {
        super.init();

        int centerX = this.width / 2 + 10;
        int y = (this.height - panelHeight) / 2 + PADDING + 45;

        // Subtopic Name input
        this.subtopicNameInput = new EditBox(this.font, centerX - 100, y, 200, INPUT_HEIGHT, Component.literal("Subtopic Name"));
        this.subtopicNameInput.setMaxLength(100);
        this.addRenderableWidget(subtopicNameInput);
        this.setInitialFocus(subtopicNameInput);

        // Position X
        y += INPUT_HEIGHT + 12;
        this.posXInput = new EditBox(this.font, centerX - 100, y, 95, INPUT_HEIGHT, Component.literal("Position X"));
        posXInput.setValue("0");
        posXInput.setMaxLength(5);
        this.addRenderableWidget(posXInput);

        // Position Y
        this.posYInput = new EditBox(this.font, centerX + 5, y, 95, INPUT_HEIGHT, Component.literal("Position Y"));
        posYInput.setValue("0");
        posYInput.setMaxLength(5);
        this.addRenderableWidget(posYInput);

        // Icon input
        y += INPUT_HEIGHT + 12;
        this.iconInput = new EditBox(this.font, centerX - 100, y, 200, INPUT_HEIGHT, Component.literal("Icon (e.g. minecraft:comparator)"));
        iconInput.setValue("minecraft:compass");
        this.addRenderableWidget(iconInput);

        // Create button
        y += INPUT_HEIGHT + 20;
        this.addRenderableWidget(Button.builder(Component.literal("Create"), button -> onCreate())
                .pos(centerX - 60, y)
                .size(60, INPUT_HEIGHT)
                .build());

        // Cancel button
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .pos(centerX + 5, y)
                .size(55, INPUT_HEIGHT)
                .build());
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        previousScreen.render(gg, mouseX, mouseY, partialTicks);
        renderBlurredBackground(partialTicks);

        // Draw background
        gg.fill(0, 0, this.width, this.height, 0xCC000000);

        // Draw panel
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        gg.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF222228);
        gg.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF666666);

        // Draw title
        String title = "Add Subtopic to " + topic.getTitle();
        gg.drawCenteredString(this.font, title, this.width / 2, panelY + 20, 0xFFFFFF);

        // Draw labels
        int labelX = panelX + PADDING;
        int startY = panelY + PADDING + 50;

        gg.drawString(this.font, "Name:", labelX, startY, 0xFFCCCCCC);

        startY += INPUT_HEIGHT + 12;
        gg.drawString(this.font, "Position:", labelX, startY, 0xFFCCCCCC);

        startY += INPUT_HEIGHT + 12;
        gg.drawString(this.font, "Icon:", labelX, startY, 0xFFCCCCCC);

        for (Renderable renderable : renderables) {
            renderable.render(gg, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            onCreate();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void onCreate() {
        String subtopicName = subtopicNameInput.getValue().trim();

        if (subtopicName.isEmpty()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_TAKE_RESULT, 1.0f));
            return;
        }

        // Parse position
        int posX, posY;
        try {
            posX = Integer.parseInt(posXInput.getValue());
            posY = Integer.parseInt(posYInput.getValue());
        } catch (NumberFormatException e) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_TAKE_RESULT, 1.0f));
            return;
        }

        String iconStr = iconInput.getValue().trim();
        if (iconStr.isEmpty()) {
            iconStr = "minecraft:compass";
        }

        // Create new YAML entry
        Map<String, Object> yamlData = new LinkedHashMap<>();
        yamlData.put("title", subtopicName);
        yamlData.put("posX", posX);
        yamlData.put("posY", posY);
        yamlData.put("icon", iconStr);
        yamlData.put("pre", java.util.List.of());
        yamlData.put("content", "# " + subtopicName + "\n\nAdd your content here.");

        // Create a unique ID for the subtopic
        String subtopicId = subtopicName.toLowerCase().replace(" ", "_");
        String filename = subtopicId + ".yaml";

        ResourceLocation subtopicLocation = ResourceLocation.tryParse(
                topic.getContentLocation().getNamespace() + ":" + topic.getContentLocation().getPath() + "/" + filename
        );

        if (subtopicLocation == null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_TAKE_RESULT, 1.0f));
            return;
        }

        // Write the YAML file
        boolean success = MarkdownFileManager.writeYamlData(subtopicLocation, yamlData);

        if (success) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));

            // Add the subtopic to the topic in memory
            TopicManager.getInstance().addSubtopicToTopic(
                    topic,
                    subtopicName,
                    ResourceLocation.tryParse(iconStr),
                    "# " + subtopicName + "\n\nAdd your content here.",
                    posX,
                    posY,
                    subtopicLocation
            );

            // Return to previous screen
            Minecraft.getInstance().setScreen(previousScreen);
        } else {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_TAKE_RESULT, 1.0f));
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(previousScreen);
    }

}



