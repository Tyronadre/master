package de.tyro.mcnetwork.networkBook.data;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Topic = main category */
public class Topic {

    private final String title;
    private final ResourceLocation icon;
    private final List<Subtopic> subtopics = new ArrayList<>();

    public Topic(String title, String iconPath) {
        this.title = title;
        this.icon = iconPath == null ? null : ResourceLocation.parse(iconPath);
    }

    public String getTitle() { return title; }
    public ResourceLocation getIcon() { return icon; }

    public void addSubtopic(Subtopic s) { subtopics.add(s); }
    public List<Subtopic> getSubtopics() { return subtopics; }
}
