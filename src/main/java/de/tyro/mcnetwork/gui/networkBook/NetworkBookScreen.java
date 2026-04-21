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

import static de.tyro.mcnetwork.Config.DEV_MODE;

public class NetworkBookScreen extends Screen {

    private final Minecraft mc = Minecraft.getInstance();

    public static final int BACKGROUND_COLOR = 0xFF222228;
    public static final int TEXT_COLOR = 0xFFFFFF;

    // Managers & renderers
    private final TopicManager topicManager = TopicManager.getInstance();

    // UI components
    private IconTabBar tabBar;
    private DraggablePlane draggablePlane;
    private ContentPane contentPane;

    // state
    private static Topic currentTopic;
    private static SubTopic currentSubtopic;
    private boolean initialized;

    // animation helpers
    private float transitionProgress = 1.0f;

    public NetworkBookScreen() {
        super(Component.literal("Netzwerkhandbuch"));
    }

    @Override
    public void init() {
        super.init();

        tabBar = new IconTabBar(8, 16, 40, this::onTabClicked);
        var topics = topicManager.getTopics();
        tabBar.setTopics(topics);

        currentTopic = topicManager.getLastTopic();

        draggablePlane = new DraggablePlane(this, 64, 24, this.width - 64 - 18, this.height - 48);
        if (currentTopic != null) draggablePlane.setSubtopics(currentTopic.getSubtopics());

        contentPane = new ContentPane(64, 24, this.width - 64 - 18, this.height - 48);
        if (currentSubtopic != null) contentPane.setSubtopic(currentSubtopic);
        contentPane.setCloseListener(c -> closeSubtopic());
        contentPane.setCompletionListener(this::onMarkComplete);

        // Add edit button (only in dev environments)
        if (DEV_MODE.getAsBoolean())
            this.addRenderableWidget(Button.builder(Component.literal("Edit"), button -> this.onEditClicked())
                    .pos(this.width - 60, 8)
                    .size(50, 16)
                    .build());

        this.initialized = true;
    }

    void onTabClicked(Topic clicked) {
        if (clicked == null) return;

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
        currentTopic = clicked;
        currentSubtopic = null;
        topicManager.setLastTopic(clicked);
        draggablePlane.setSubtopics(currentTopic.getSubtopics());

        this.transitionProgress = 0.0f;
    }

    void onSubtopicClicked(SubTopic s) {
        currentSubtopic = s;
        contentPane.setSubtopic(s);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));

        this.transitionProgress = 0.0f;
    }

    void closeSubtopic() {
        currentSubtopic = null;
        this.transitionProgress = 0.0f;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }

    void onMarkComplete(SubTopic s) {
        topicManager.markCompleted(mc.player, s);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0f));
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        super.render(gg, mouseX, mouseY, partialTicks);


        String title = currentTopic == null ? "No Topic" : currentTopic.getTitle();
        if (currentSubtopic != null) title += " - " + currentSubtopic.getTitle();
        gg.drawCenteredString(this.font, title, this.width / 2, 8, 0xFFFFFF);

        tabBar.render(gg, mouseX, mouseY, partialTicks);

        gg.flush();

        gg.enableScissor(64, 24, this.width - 18, this.height - 24);

        if (currentSubtopic == null) {
            draggablePlane.render(gg, mouseX, mouseY, false);
        } else {
            contentPane.render(gg, mouseX, mouseY, partialTicks);
        }

        gg.disableScissor();
    }

    //TODO maybe we register the click, but only open the topic when a user releases ontop of it again. otherwise it might be a pain to drag
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isDragging()) return false;
        if (tabBar.mouseClicked(mouseX, mouseY, button)) return true;
        if (currentSubtopic == null) {
            if (draggablePlane.mouseClicked(mouseX, mouseY, button)) return true;
        } else {
            if (contentPane.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (currentSubtopic == null) {
            if (draggablePlane.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        } else {
            if (contentPane.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (currentSubtopic != null) {
            return contentPane.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (currentSubtopic != null && contentPane != null) {
            if (contentPane.charTyped(codePoint, modifiers)) return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currentSubtopic != null && contentPane != null) {
            if (contentPane.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeSubtopic();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    void onEditClicked() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        Minecraft.getInstance().setScreen(new MarkdownEditorSelectorScreen(this));
    }
}
