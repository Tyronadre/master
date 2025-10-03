package de.tyro.mcnetwork.gui.networkBook;


import de.tyro.mcnetwork.networkBook.data.Subtopic;
import de.tyro.mcnetwork.networkBook.data.Topic;
import de.tyro.mcnetwork.networkBook.data.TopicManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Main GUI screen for the Network Book.
 * - Left: IconTabBar (icons only)
 * - Right: single content window (either TopicView with DraggablePlane OR SubtopicView with ContentPane)
 *
 * State:
 * - currentTopic (selected main topic)
 * - currentSubtopic (optional; if present, show SubtopicView)
 *
 * Animations and hooks are modular and kept as placeholders (transitionProgress, etc.)
 */
public class NetworkBookScreen extends Screen {

    private final Minecraft mc = Minecraft.getInstance();

    // Managers & renderers
    private final TopicManager topicManager = TopicManager.getInstance();
    private final MarkdownRenderer markdownRenderer = new MarkdownRenderer(mc);

    // UI components
    private IconTabBar tabBar;
    private DraggablePlane draggablePlane;
    private ContentPane contentPane;

    // state
    private Topic currentTopic;
    private Subtopic currentSubtopic; // null if showing topic view

    // animation helpers
    private float transitionProgress = 1.0f; // 0..1 for transitions

    public NetworkBookScreen() {
        super(Component.literal("Netzwerkhandbuch"));
    }

    @Override
    protected void init() {
        super.init();

        // Load topics (from JSON/resource) - synchronous for blueprint; replace with async if heavy
        topicManager.loadAll();

        // initialize UI components
        tabBar = new IconTabBar(8, 16, 40, this::onTabClicked);
        tabBar.setTopics(topicManager.getTopics());

        // default to first topic if available
        Optional<Topic> maybe = topicManager.getTopics().stream().findFirst();
        currentTopic = maybe.orElse(null);

        // create draggable plane (topic view)
        draggablePlane = new DraggablePlane(64, 24, this.width - 64 - 18, this.height - 48);
        if (currentTopic != null) draggablePlane.setSubtopics(currentTopic.getSubtopics());

        // content pane (subtopic view)
        contentPane = new ContentPane(64, 24, this.width - 64 - 18, this.height - 48, markdownRenderer);
        contentPane.setCloseListener(c -> closeSubtopic());
        contentPane.setCompletionListener(this::onMarkComplete);

        // add a simple button for debug/closing (optional)
//        this.addRenderableWidget(Button.builder(Component.literal("Close GUI"), btn -> onClose()).bounds(this.width - 26, 6, 20, 20).build());
    }

    private void onTabClicked(Topic clicked) {
        // start transition animation
        if (clicked == null || clicked.equals(currentTopic)) return;

        // set new topic, update draggable plane content
        this.currentTopic = clicked;
        this.currentSubtopic = null; // ensure topic view
        draggablePlane.setSubtopics(currentTopic.getSubtopics());

        // play slide animation (placeholder)
        this.transitionProgress = 0.0f;
    }

    private void onSubtopicClicked(Subtopic s) {
        // open subtopic: start transition -> open contentPane with that subtopic
        this.currentSubtopic = s;
        contentPane.setSubtopic(s);
        // animation placeholder
        this.transitionProgress = 0.0f;
    }

    private void closeSubtopic() {
        // close subtopic and return to topic view
        this.currentSubtopic = null;
        // animation placeholder
        this.transitionProgress = 0.0f;
    }

    private void onMarkComplete(Subtopic s) {
        topicManager.markCompleted(mc.player, s);
        // reflect visually on draggablePlane
        draggablePlane.refreshCompletionState();
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        super.render(gg, mouseX, mouseY, partialTicks);

        // Top-left title:
        String title = currentTopic == null ? "No Topic" : currentTopic.getTitle();
        if (currentSubtopic != null) title += " - " + currentSubtopic.getTitle();
        gg.drawCenteredString(this.font, title, this.width / 2, 8, 0xFFFFFF);

        // left tab bar
        tabBar.render(gg, mouseX, mouseY, partialTicks);

        // content: either DraggablePlane (topic view) or ContentPane (subtopic view)
        if (currentSubtopic == null) {
            // Topic view shown
            draggablePlane.render(gg, mouseX, mouseY, partialTicks, tile -> onSubtopicClicked(tile.getSubtopic()));
        } else {
            // Subtopic view shown (vertical scroll + close + bottom action)
            contentPane.render(gg, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // delegate to left bar or content
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
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

//    @Override
//    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
//        // only allow wheel when in subtopic view
//        if (currentSubtopic != null) {
//            return contentPane.mouseScrolled(mouseX, mouseY, delta) || super.mouseScrolled(mouseX, mouseY, delta);
//        }
//        return super.mouseScrolled(mouseX, mouseY, delta);
//    }

    @Override
    public boolean isPauseScreen() {
        return false; // not pausing the game
    }
}
