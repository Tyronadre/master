package de.tyro.mcnetwork.networkBook.data;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Utility class for managing YAML markdown files used in network book chapters.
 * Handles both reading from resources and writing to disk.
 */
public class MarkdownFileManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Yaml YAML = new Yaml();
    private static final ResourceManager RESOURCE_MANAGER = Minecraft.getInstance().getResourceManager();

    /**
     * Reads the content field from a YAML file.
     */
    public static String readMarkdownContent(ResourceLocation location) {
        var resource = RESOURCE_MANAGER.getResource(location);
        if (resource.isEmpty()) return null;
        try (BufferedReader reader = resource.get().openAsReader()) {
            Map<String, Object> data = YAML.load(reader);
            Object content = data.get("content");
            return content != null ? content.toString() : "";
        } catch (Exception e) {
            LOGGER.error("Error reading markdown content from {}", location, e);
            return "";
        }
    }

    /**
     * Writes the content field back to a YAML file.
     * Preserves all other fields in the YAML.
     */
    public static boolean writeMarkdownContent(ResourceLocation location, String newContent) {

        var resource = RESOURCE_MANAGER.getResource(location);
        if (resource.isEmpty()) return false;
        try (BufferedReader reader = resource.get().openAsReader()) {
            Map<String, Object> data = YAML.load(reader);
            data.put("content", newContent);

            for (File it : List.of(
                    new File("../build/resources/main/assets/" + location.getNamespace() + "/" + location.getPath()),
                    new File("../src/main/resources/assets/" + location.getNamespace() + "/" + location.getPath())
            )) {
                try (BufferedWriter writer = Files.newBufferedWriter(it.toPath())) {
                    YAML.dump(data, writer);
                    LOGGER.info("Successfully saved markdown content to {}", it.getAbsoluteFile());
                } catch (Exception e) {
                    LOGGER.error("Error writing markdown content to {}", it.getAbsoluteFile(), e);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error writing markdown content to {}", location, e);
            return false;
        }
        return true;
    }

}




