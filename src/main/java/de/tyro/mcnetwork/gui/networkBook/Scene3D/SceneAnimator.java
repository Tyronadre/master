package de.tyro.mcnetwork.gui.networkBook.Scene3D;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class SceneAnimator {
    private record Packet(Vector3f from, Vector3f to, long startTime, long duration, int color, String label) {
    }

    private final List<Packet> packets = new ArrayList<>();

    public void load(SceneDefinition def, JsonArray arr) {
        long now = System.currentTimeMillis();
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            if (!o.get("type").getAsString().equals("packet_flight")) continue;
            Vector3f from;
            if (o.has("from")) {
                from = def.getBlockWithLabel(o.get("from").getAsString()).getPos();
            } else {
                from = new Vector3f(o.get("from_x").getAsFloat(), o.get("from_y").getAsFloat(), o.get("from_z").getAsFloat());
            }

            Vector3f to;
            if (o.has("to")) {
                to = def.getBlockWithLabel(o.get("to").getAsString()).getPos();
            } else {
                to = new Vector3f(o.get("to_x").getAsFloat(), o.get("to_y").getAsFloat(), o.get("to_z").getAsFloat());
            }

            int color = Color.decode(o.get("color").getAsString()).getRGB();
            long duration = o.get("duration").getAsLong();
            packets.add(new Packet(from, to, now, duration, color, o.get("label").getAsString()));
        }
    }

    public void renderPackets(PoseStack pose, MultiBufferSource.BufferSource buffer, float partialTicks) {
        long now = System.currentTimeMillis();

        for (Packet p : packets) {
            float t = (now - p.startTime) / (float) p.duration;
            if (t > 1f) continue;

            Vector3f pos = new Vector3f(p.from).lerp(p.to, t);

//            VertexConsumer vc = buffer.getBuffer(RenderType.lines());
//            vc.addVertex(pose.last().pose(), pos.x, pos.y, pos.z)
//                    .setColor(p.color >> 16 & 255, p.color >> 8 & 255, p.color & 255, 255);
//            vc.addVertex(pose.last().pose(), pos.x, pos.y + 0.2f, pos.z)
//                    .setColor(p.color >> 16 & 255, p.color >> 8 & 255, p.color & 255, 255);
        }
    }
}