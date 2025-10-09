package de.tyro.mcnetwork.gui.networkBook.Scene3D;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.util.*;

public class SceneDefinition {
    public final List<SceneBlock> blocks = new ArrayList<>();
    public final SceneAnimator animator = new SceneAnimator();

    public static SceneDefinition load(ResourceLocation path) {
        Minecraft mc = Minecraft.getInstance();
        try {
            Resource res = mc.getResourceManager().getResource(path).orElse(null);
            if (res == null) throw new RuntimeException("Missing scene: " + path);

            JsonObject root = JsonParser.parseReader(new InputStreamReader(res.open())).getAsJsonObject();
            SceneDefinition def = new SceneDefinition();

            for (JsonElement el : root.getAsJsonArray("objects")) {
                JsonObject obj = el.getAsJsonObject();
                BlockState state = Blocks.AIR.defaultBlockState();
                def.blocks.add(new SceneBlock(
                        state,
                        obj.get("x").getAsFloat(),
                        obj.get("y").getAsFloat(),
                        obj.get("z").getAsFloat(),
                        obj.get("label").getAsString()
                ));
            }

            def.animator.load(def, root.getAsJsonArray("animations"));
            return def;

        } catch (Exception e) {
            throw new RuntimeException("Error loading scene JSON: " + path, e);
        }
    }

    public SceneBlock getBlockWithLabel(String label) {
        return blocks.stream().filter(it -> Objects.equals(label, it.label())).findFirst().orElse(null);
    }

    public record SceneBlock(BlockState state, float x, float y, float z, String label) {
        Vector3f getPos() {
            return new Vector3f(x, y, z);
        }
    }
}