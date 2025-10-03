package de.tyro.mcnetwork.networkBook.data;

import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Subtopic represents a single lesson */
public class Subtopic {

    private final String id;
    private final String title;
    private final String content;
    private final Vec2 position;
    private final List<Subtopic> prerequisites = new ArrayList<>();

    public Subtopic(String title, String content, Vec2 position) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.content = content;
        this.position = position;
    }

    public void addPrerequisite(Subtopic subtopic) {
        if (!prerequisites.contains(subtopic)) {
            prerequisites.add(subtopic);
        }
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
}
