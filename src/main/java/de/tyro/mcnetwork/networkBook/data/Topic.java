package de.tyro.mcnetwork.networkBook.data;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Topic = main category */
public class Topic {

    private final String title;
    private final ResourceLocation content;
    private final ResourceLocation icon;
    private final List<SubTopic> subtopics = new ArrayList<>();

    public Topic(String title, ResourceLocation icon, ResourceLocation content) {
        this.title = title;
        this.icon = icon;
        this.content = content;
    }

    public String getTitle() { return title; }
    public ResourceLocation getIcon() { return icon; }
    public ResourceLocation getContent() { return content; }

    public void addSubtopic(SubTopic s) { subtopics.add(s); }
    public List<SubTopic> getSubtopics() { return subtopics; }
}
