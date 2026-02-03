package de.tyro.mcnetwork.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.tyro.mcnetwork.routing.InFlightPacket;
import de.tyro.mcnetwork.routing.SimulationEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.Collection;

public class InFlightPacketRenderer {

    private final Minecraft mc = Minecraft.getInstance();

    public void render(PoseStack ps, MultiBufferSource buffer, float partialTicks) {
        SimulationEngine sim = SimulationEngine.getInstance();
        Font font = Minecraft.getInstance().font;

        if (sim == null) return;

        for (InFlightPacket packet : sim.getInFlightPackets()) {
            var pos = packet.getCurrentPosition();
            double x = pos.x();
            double y = pos.y();
            double z = pos.z();

            ps.pushPose();
            ps.translate(x, y + 1.0, z); // leicht über der Node
            ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation()); // always face player
            ps.scale(-0.025f, -0.025f, 0.025f); // skalieren

            renderPacket(ps, buffer, packet, font);

            ps.popPose();
        }
    }

    private void renderPacket(PoseStack ps, MultiBufferSource buffer, InFlightPacket inFlight, Font font) {
        float padding = 2f;

        Iterable<String> content = inFlight.packet.getRenderContent();
        String headerLeft = inFlight.packet.getPacketTypeName();
        String headerRight = String.valueOf(inFlight.packet.id);

        // Berechne Höhe
        int lineCount = 1 + 1 + ((Collection<?>) content).size(); // header + IP + content
        int lineHeight = font.lineHeight;
        float width = 100f; // statisch oder dynamisch berechnen
        float height = lineHeight * lineCount + padding * 2;

        // Hintergrund: halbtransparent schwarz
//        RenderSystem.bindTexture();
        RenderSystem.setShaderColor(0f, 0f, 0f, 0.5f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0f, 0f, 0f, 0.5f);
        // Rechteck zeichnen
        drawRect(ps, -width / 2, 0, width, height, 0,0,0,0.5f);
//        RenderSystem.enableTexture();

        float yOffset = padding;

        // Header: PacketType links, ID rechts
        drawString(headerLeft, -width / 2 + padding, yOffset, 0xFFFFFF, ps, font);
        drawString(headerRight, width/2 -padding - font.width(headerRight), yOffset, 0xFFFFFF, ps, font);
        yOffset += lineHeight;

        // IP Zeile: Quelle → Ziel
        String ipLine = "";
        try {
            ipLine = inFlight.packet.getClass().getMethod("getSourceIp").invoke(inFlight.packet) + " → " +
                    inFlight.packet.getClass().getMethod("getDestinationIp").invoke(inFlight.packet);
        } catch (Exception ignored) {
        }

        drawString(ipLine, -width / 2 + padding, yOffset, 0xFFFFFF, ps, font);
        yOffset += lineHeight;

        // Content Zeilen
        for (String line : content) {
            drawString(line, -width / 2 + padding, yOffset, 0xFFFFFF, ps, font);
            yOffset += lineHeight;
        }
    }

    private void drawString(String string, float x, float y, int color, PoseStack pose, Font font) {
        font.drawInBatch(string, x, y, 0xFFFFFF, true, pose.last().pose(), Minecraft.getInstance().renderBuffers().bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880, font.isBidirectional());
    }

    private void drawRect(PoseStack ps, float x, float y, float width, float height, float r, float g, float b, float a) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, a);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buffer = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        buffer.addVertex(ps.last().pose(), x, y, 0).setColor(r, g, b, a);
        buffer.addVertex(ps.last().pose(), x, y + height, 0).setColor(r, g, b, a);
        buffer.addVertex(ps.last().pose(), x + width, y + height, 0).setColor(r, g, b, a);
        buffer.addVertex(ps.last().pose(), x + width, y, 0).setColor(r, g, b, a);

        RenderSystem.disableBlend();
    }
}