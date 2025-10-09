package de.tyro.mcnetwork.gui.networkBook.Scene3D;

import com.google.gson.*;
import de.tyro.mcnetwork.block.BlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.io.InputStreamReader;
import java.io.IOException;
import java.util.*;

/**
 * SceneRenderer — renders interactive network scenes defined in JSON files.
 *
 * Supports:
 *  - Static block objects with labels (Client / Server)
 *  - Simple "packet_flight" animations (request / response)
 *  - Dynamic timing synchronized to game ticks
 *
 * JSON format example in resources/assets/modid/scenes/network/http_request_scene.json:
 *
 * {
 *   "width": 180,
 *   "height": 100,
 *   "objects": [
 *     { "id": "server", "type": "block", "block": "minecraft:diamond_block", "x": 140, "y": 40, "label": "Server" },
 *     { "id": "client", "type": "block", "block": "minecraft:gold_block", "x": 20, "y": 40, "label": "Client" }
 *   ],
 *   "animations": [
 *     { "type": "packet_flight", "from": "client", "to": "server", "duration": 80, "color": "#00AAFF", "label": "Request" },
 *     { "type": "packet_flight", "from": "server", "to": "client", "duration": 80, "delay": 90, "color": "#55FF55", "label": "Response" }
 *   ]
 * }
 */
public class AnimationRenderer {

    private static final Map<ResourceLocation, AnimationRenderer> CACHE = new HashMap<>();

    private final List<SceneObject> objects = new ArrayList<>();
    private final List<SceneAnimation> animations = new ArrayList<>();
    private final int width;
    private final int height;

    private AnimationRenderer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // ============================================================
    // === PUBLIC API =============================================
    // ============================================================

    public static AnimationRenderer load(ResourceLocation path) {
        if (CACHE.containsKey(path)) return CACHE.get(path);
        Minecraft mc = Minecraft.getInstance();

        try {
            Resource res = mc.getResourceManager().getResource(path).orElse(null);
            if (res == null) {
                throw new IOException("Scene file not found: " + path);
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(res.open())).getAsJsonObject();
            int width = json.has("width") ? json.get("width").getAsInt() : 200;
            int height = json.has("height") ? json.get("height").getAsInt() : 100;
            AnimationRenderer scene = new AnimationRenderer(width, height);

            // --- Parse objects ---
            JsonArray objs = json.getAsJsonArray("objects");
            if (objs != null) {
                for (JsonElement e : objs) {
                    JsonObject o = e.getAsJsonObject();
                    String id = o.get("id").getAsString();
                    String blockId = o.get("block").getAsString();
                    int x = o.get("x").getAsInt();
                    int y = o.get("y").getAsInt();
                    String label = o.has("label") ? o.get("label").getAsString() : "";
                    scene.objects.add(new SceneObject(id, blockId, x, y, label));
                }
            }

            // --- Parse animations ---
            JsonArray anims = json.getAsJsonArray("animations");
            if (anims != null) {
                for (JsonElement e : anims) {
                    JsonObject a = e.getAsJsonObject();
                    String type = a.get("type").getAsString();
                    if (type.equals("packet_flight")) {
                        scene.animations.add(PacketFlightAnimation.fromJson(a));
                    }
                }
            }

            CACHE.put(path, scene);
            return scene;

        } catch (IOException e) {
            throw new RuntimeException("Error loading scene JSON: " + path, e);
        }
    }

    public void render(GuiGraphics gg, int x, int y, float time) {
        PoseStack ps = gg.pose();

        // --- Draw static objects ---
        for (SceneObject obj : objects) {
            obj.render(gg, x, y);
        }

        // --- Draw animations ---
        for (SceneAnimation anim : animations) {
            anim.render(gg, x, y, time, objects);
        }
    }

    public int getHeight() {
        return height;
    }

    // ============================================================
    // === SCENE OBJECT ===========================================
    // ============================================================

    private static class SceneObject {
        final String id;
        final BlockState blockState;
        final int x, y;
        final String label;

        SceneObject(String id, String blockName, int x, int y, String label) {
            this.id = id;
            Block block = Block.byItem(Minecraft.getInstance().player.getInventory().getSelected().getItem());
            try {
                block = BlockRegistry.COMPUTER.get();;
            } catch (Exception ignored) {}
            this.blockState = block.defaultBlockState();
            this.x = x;
            this.y = y;
            this.label = label;
        }

        void render(GuiGraphics gg, int baseX, int baseY) {
            PoseStack ps = gg.pose();
            ps.pushPose();
            ps.translate(baseX + x, baseY + y, 0);
            ps.scale(20f, 20f, 20f); // make block visible in GUI scale

            BlockRenderDispatcher brd = Minecraft.getInstance().getBlockRenderer();
            MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
            brd.renderSingleBlock(blockState, ps, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.solid());
            buffer.endBatch();

            ps.popPose();

            if (!label.isEmpty()) {
                gg.drawCenteredString(Minecraft.getInstance().font, label, baseX + x + 10, baseY + y + 35, 0xFFFFFF);
            }
        }
    }

    // ============================================================
    // === BASE ANIMATION CLASS ===================================
    // ============================================================

    private abstract static class SceneAnimation {
        abstract void render(GuiGraphics gg, int baseX, int baseY, float time, List<SceneObject> objects);

        SceneObject find(List<SceneObject> objs, String id) {
            for (SceneObject o : objs) if (o.id.equals(id)) return o;
            return null;
        }
    }

    // ============================================================
    // === PACKET FLIGHT ANIMATION ================================
    // ============================================================

    private static class PacketFlightAnimation extends SceneAnimation {
        final String from, to, label;
        final int duration, delay;
        final int color;

        PacketFlightAnimation(String from, String to, int duration, int delay, int color, String label) {
            this.from = from;
            this.to = to;
            this.duration = duration;
            this.delay = delay;
            this.color = color;
            this.label = label;
        }

        static PacketFlightAnimation fromJson(JsonObject json) {
            String from = json.get("from").getAsString();
            String to = json.get("to").getAsString();
            int duration = json.has("duration") ? json.get("duration").getAsInt() : 60;
            int delay = json.has("delay") ? json.get("delay").getAsInt() : 0;
            String label = json.has("label") ? json.get("label").getAsString() : "";
            int color = 0x00AAFF;
            if (json.has("color")) {
                try {
                    color = Integer.parseInt(json.get("color").getAsString().replace("#", ""), 16) | 0xFF000000;
                } catch (Exception ignored) {}
            }
            return new PacketFlightAnimation(from, to, duration, delay, color, label);
        }

        @Override
        void render(GuiGraphics gg, int baseX, int baseY, float time, List<SceneObject> objects) {
            float gameTime = (Minecraft.getInstance().level.getGameTime() % (duration + delay * 2));
            float localTime = gameTime - delay;
            if (localTime < 0 || localTime > duration) return;

            float t = localTime / duration;

            SceneObject a = find(objects, from);
            SceneObject b = find(objects, to);
            if (a == null || b == null) return;

            int ax = baseX + a.x + 16;
            int ay = baseY + a.y + 16;
            int bx = baseX + b.x + 16;
            int by = baseY + b.y + 16;

            int px = (int) (ax + (bx - ax) * t);
            int py = (int) (ay + (by - ay) * t);

            gg.fill(px - 2, py - 2, px + 2, py + 2, color);
            if (!label.isEmpty()) {
                gg.drawString(Minecraft.getInstance().font, label, px + 6, py - 4, color);
            }
        }
    }
}
