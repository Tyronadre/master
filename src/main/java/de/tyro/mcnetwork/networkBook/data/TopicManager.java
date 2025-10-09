package de.tyro.mcnetwork.networkBook.data;

import com.mojang.logging.LogUtils;
import de.tyro.mcnetwork.MCNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simple TopicManager. In production, load JSONs from resources (Gson/Jackson) and fill Topic/Subtopic objects.
 * Also persist completion state per player (e.g. PlayerPersistentData / NBT).
 */
public class TopicManager {
    public static final Logger logger = LogUtils.getLogger();
    public static final ResourceManager rm = Minecraft.getInstance().getResourceManager();

    private static final String CHAPTER_FOLDER = "networkbook/";
    private static final TopicManager INSTANCE = new TopicManager();
    private final List<Topic> topics = new ArrayList<>();


    private TopicManager() {
    }

    public static TopicManager getInstance() {
        return INSTANCE;
    }

    public void loadTopics() {
        var location = ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, CHAPTER_FOLDER + "chapters.yaml");
        var resourceO = rm.getResource(location);
        if (resourceO.isEmpty()) return;

        try (var r = resourceO.get().openAsReader()) {
            List<Map<String, Object>> chapters = new Yaml().load(r);
            for (var chapter : chapters) {
                Topic topic = new Topic((String) chapter.get("title"), ResourceLocation.parse((String) chapter.get("icon")), ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, CHAPTER_FOLDER + chapter.get("path")));
                loadTopic(topic);
            }
        } catch (Exception e) {
            logger.error("Failed to load topics.", e);
        }
    }

    private void loadTopic(Topic topic) {
        topics.add(topic);

        var files = rm.listResources(topic.getContentLocation().getPath(), it ->it.getPath().endsWith(".yaml"));
        for (var entry : files.entrySet()) {
            try (var r = entry.getValue().openAsReader()) {
                Map<String, Object> map = new Yaml().load(r);
                new SubTopic(topic, (String) map.get("title"), (String) map.get("icon"), (String) map.get("content"), (int) map.get("posX"), (int) map.get("posY"), topic.getContentLocation());
            } catch (Exception e) {
                logger.error("Failed to load topic at {}", entry.getKey().getPath(), e);
            }
            logger.info("Loaded topic at {}", entry.getKey().getPath());
        }
    }

    public List<Topic> getTopics() {
        topics.clear();
        loadTopics();
        return topics;
    }

    public void markCompleted(Player player, SubTopic subtopic) {
        // Example: store completion in player's persistent NBT under "networkbook_completed"
        if (player == null) return;
        CompoundTag tag = player.getPersistentData();
        CompoundTag mod = tag.getCompound("mod_networkbook");
        mod.putBoolean("completed:" + subtopic.getId(), true);
        tag.put("mod_networkbook", mod);
    }

    public boolean isCompleted(Player player, SubTopic subtopic) {
        if (player == null) return false;
        CompoundTag tag = player.getPersistentData();
        CompoundTag mod = tag.getCompound("mod_networkbook");
        return mod.getBoolean("completed:" + subtopic.getId());
    }
}
