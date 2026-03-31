package de.tyro.mcnetwork.networkBook.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** Topic = main category */
public class Topic extends SavedData {

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
    public ResourceLocation getContentLocation() { return content; }

    public void addSubtopic(SubTopic s) { subtopics.add(s); }
    public List<SubTopic> getSubtopics() { return subtopics; }

    public SubTopic getSubTopicByTitle(String title) {
        for (SubTopic s : subtopics) {
            if (s.getTitle().equals(title)) { return s; }
        }
        return null;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        tag.putString("title", title);
        tag.putString("icon", icon.toString());
        tag.putInt("subtopics", subtopics.size());
        for (int i = 0; i < subtopics.size(); i++) {
            tag.putUUID("sub_"+i, subtopics.get(i).getId());
        }

        return tag;
    }

    public static Topic load(CompoundTag tag, HolderLookup.Provider registries) {
        var topic = new Topic(
                tag.getString("title"),
                ResourceLocation.parse(tag.getString("icon")),
                ResourceLocation.parse(tag.getString("content"))
                );
        var subtopics = tag.getInt("subtopics");
        for (int i = 0; i < subtopics; i++) {
        }

        return topic;
    }





}
