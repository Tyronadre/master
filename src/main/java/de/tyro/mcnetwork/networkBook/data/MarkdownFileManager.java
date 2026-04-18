package de.tyro.mcnetwork.networkBook.data;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
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
    private static final Yaml YAML;
    private static final ResourceManager RESOURCE_MANAGER = Minecraft.getInstance().getResourceManager();

    static {
        DumperOptions options = new DumperOptions();
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.JSON_SCALAR_STYLE);
        options.setLineBreak(DumperOptions.LineBreak.UNIX);
        YAML = new Yaml(options);
    }

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
     * Writes all YAML fields back to a YAML file.
     * Updates: title, posX, posY, icon, pre (prerequisites), and content.
     */
    public static boolean writeYamlData(ResourceLocation location, Map<String, Object> data) {
        try {
            for (File it : List.of(
                    new File("../build/resources/main/assets/" + location.getNamespace() + "/" + location.getPath()),
                    new File("../src/main/resources/assets/" + location.getNamespace() + "/" + location.getPath())
            )) {
                try (BufferedWriter writer = Files.newBufferedWriter(it.toPath())) {
                    YAML.dump(data, writer);
                    LOGGER.info("Successfully saved YAML data to {}", it.getAbsoluteFile());
                } catch (Exception e) {
                    LOGGER.error("Error writing YAML data to {}", it.getAbsoluteFile(), e);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error writing YAML data to {}", location, e);
            return false;
        }
        return true;
    }

    /**
     * Reads all YAML data from a file.
     */
    public static Map<String, Object> readYamlData(ResourceLocation location) {
        var resource = RESOURCE_MANAGER.getResource(location);
        if (resource.isEmpty()) return null;
        try (BufferedReader reader = resource.get().openAsReader()) {
            return YAML.load(reader);
        } catch (Exception e) {
            LOGGER.error("Error reading YAML data from {}", location, e);
            return null;
        }
    }

    /**
     * Writes the content field back to a YAML file.
     * Preserves all other fields in the YAML (convenience method).
     */
    public static boolean writeMarkdownContent(ResourceLocation location, String newContent) {
        Map<String, Object> data = readYamlData(location);
        if (data == null) return false;
        data.put("content", newContent);
        return writeYamlData(location, data);
    }

}




