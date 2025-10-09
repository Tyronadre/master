package de.tyro.mcnetwork.networkBook.data;

import de.tyro.mcnetwork.networkBook.markdown.MarkdownParser;
import de.tyro.mcnetwork.networkBook.markdown.MarkdownRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Subtopic represents a single lesson
 */
public class SubTopic {
    private final Topic topic;
    private final String id;
    private final String title;
    private final Vec2 position;
    private final List<SubTopic> prerequisites = new ArrayList<>();
    private final MarkdownRenderer markdown;
    private final ResourceLocation icon;

    public SubTopic(Topic topic, String title, ResourceLocation icon, String content, int posX, int posY) {
        this.id = UUID.randomUUID().toString();
        this.topic = topic;
        this.title = title;
        this.position = new Vec2(posX, posY);
        this.icon = icon;
        markdown = MarkdownParser.parse(content, topic.getContentLocation());
        topic.addSubtopic(this);
    }

    public void addPrerequisite(SubTopic subtopic) {
        if (!prerequisites.contains(subtopic)) {
            prerequisites.add(subtopic);
        }
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

    public MarkdownRenderer.MarkdownDocument getMarkdownDocument() {
        return markdown.getDocument();
    }

    public MarkdownRenderer getMarkdownRenderer() {
        return markdown;
    }

    public boolean isCompleted() {
        return TopicManager.getInstance().isCompleted(Minecraft.getInstance().player, this);
    }

    public ResourceLocation getIcon() {
        return icon;
    }
}
