package de.tyro.mcnetwork.networkBook.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Simple TopicManager. In production, load JSONs from resources (Gson/Jackson) and fill Topic/Subtopic objects.
 * Also persist completion state per player (e.g. PlayerPersistentData / NBT).
 */
public class TopicManager {

    private static final ResourceLocation contentDir = ResourceLocation.parse("modid:networkbook/");
    private static final TopicManager INSTANCE = new TopicManager();
    private static final Logger log = LoggerFactory.getLogger(TopicManager.class);
    private final List<Topic> topics = new ArrayList<>();


    private TopicManager() {
        loadAll();
    }

    public static TopicManager getInstance() {
        return INSTANCE;
    }

    public void loadAll() {
        loadChapter(contentDir.withSuffix("1.yaml"));
        topics.clear();

        Topic t1 = new Topic("E-Mail", "modid:textures/gui/icon_mail.png");
        t1.addSubtopic(new Subtopic("Grundlagen & Ablauf", "Hier steht der Inhalt zu SMTP...", new Vec2(0, 0)));
        t1.addSubtopic(new Subtopic("SMTP-Befehle & Statuscodes", "220\nHELO ...", new Vec2(100, 50)));
        t1.addSubtopic(new Subtopic("Header vs Envelope", "Header und Envelope Unterschiede...", new Vec2(200, 100)));

        Topic t2 = new Topic("HTTP", "modid:textures/gui/icon_http.png");
        t2.addSubtopic(new Subtopic("GET und POST", "GET/POST examples...", new Vec2(0, 0)));
        t2.addSubtopic(new Subtopic("Cookies & Sessions", "Cookie Funktionsweise...", new Vec2(100, 50)));

        topics.add(t1);
        topics.add(t2);
    }

    private void loadChapter(ResourceLocation location) {
        try {
            Map<String,Object> yaml = new Yaml().load(new InputStreamReader(Objects.requireNonNull(TopicManager.class.getResourceAsStream(location.getPath()))));
            System.out.println(yaml);
            var topic = new Topic(yaml.get("title").toString(), yaml.get("icon").toString());
            System.out.println(topic);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public List<Subtopic> findSubtopicByTitle(String title) {
        return topics.stream().flatMap(it -> it.getSubtopics().stream()).filter(it -> it.getTitle().equalsIgnoreCase(title)).toList();
    }

    public List<Topic> getTopics() {
        return topics;
    }

    public void markCompleted(Player player, Subtopic subtopic) {
        // Example: store completion in player's persistent NBT under "networkbook_completed"
        if (player == null) return;
        CompoundTag tag = player.getPersistentData();
        CompoundTag mod = tag.getCompound("mod_networkbook");
        mod.putBoolean("completed:" + subtopic.getId(), true);
        tag.put("mod_networkbook", mod);
    }

    public boolean isCompleted(Player player, Subtopic subtopic) {
        if (player == null) return false;
        CompoundTag tag = player.getPersistentData();
        CompoundTag mod = tag.getCompound("mod_networkbook");
        return mod.getBoolean("completed:" + subtopic.getId());
    }
}
