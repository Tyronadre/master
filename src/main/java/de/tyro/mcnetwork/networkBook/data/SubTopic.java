package de.tyro.mcnetwork.networkBook.data;

import de.tyro.mcnetwork.networkBook.markdown.MarkdownDocument;
import de.tyro.mcnetwork.networkBook.markdown.MarkdownParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Subtopic represents a single lesson
 */
public class SubTopic {
    private final Topic topic;
    private final String id;
    private String title;
    private Vec2 position;
    private final Set<SubTopic> prerequisites = new HashSet<>();
    private MarkdownDocument markdown;
    private ResourceLocation icon;
    private final ResourceLocation location;
    private final List<Consumer<Vec2>> positionListener = new ArrayList<>();


    public SubTopic(Topic topic, String title, ResourceLocation icon, String content, int posX, int posY, ResourceLocation location) {
        this.id = topic.getTitle() + ":" + title;
        this.topic = topic;
        this.title = title;
        this.position = new Vec2(posX, posY);
        this.icon = icon;
        this.location = location;
        markdown = MarkdownParser.parse(content, topic.getContentLocation());
        topic.addSubtopic(this);
    }

    public void addPrerequisite(SubTopic subtopic) {
        prerequisites.add(subtopic);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Vec2 getPosition() {
        return position;
    }

    public MarkdownDocument getMarkdownDocument() {
        return markdown;
    }

    public boolean isCompleted() {
        return TopicManager.getInstance().isCompleted(Minecraft.getInstance().player, this);
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public ResourceLocation getLocation() {
        return location;
    }

    public Set<SubTopic> getPrerequisite() {
        return prerequisites;
    }

    public boolean isShown() {
        if (prerequisites.isEmpty()) return true;

        return prerequisites.stream().anyMatch(SubTopic::isInteractable);
    }

    public boolean isInteractable() {
        if (prerequisites.isEmpty()) return true;

        return (prerequisites.stream().allMatch(SubTopic::isCompleted));
    }

    // Setter methods for editing YAML properties
    public void setTitle(String newTitle) {
        this.title = newTitle;
    }

    public void setPosition(int posX, int posY) {
        this.position = new Vec2(posX, posY);
        positionListener.forEach(it -> it.accept(position));
    }

    public void setIcon(ResourceLocation newIcon) {
        this.icon = newIcon;
    }

    public void updateMarkdown(String content) {
        this.markdown = MarkdownParser.parse(content, topic.getContentLocation());
    }

    public void addPositionListener(Consumer<Vec2> positionListener) {
        this.positionListener.add(positionListener);
    }

    public void clearPositionListeners() {
        this.positionListener.clear();
    }
}
