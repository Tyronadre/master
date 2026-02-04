package de.tyro.mcnetwork.routing.packet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.tyro.mcnetwork.MathUtil;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.IP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec2;

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class NetworkPacket implements INetworkPacket {

    public final UUID id = UUID.randomUUID();
    public final IP sourceIp;
    public final IP destinationIp;
    public final IP previousHopIP;

    protected NetworkPacket(IP sourceIp, IP destinationIp, IP previousHopIP) {
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        this.previousHopIP = previousHopIP;
    }

    protected NetworkPacket(IP sourceIp, IP destinationIp) {
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        previousHopIP = null;
    }

    public UUID getId() {
        return id;
    }

    public IP getSourceIp() {
        return sourceIp;
    }

    public IP getDestinationIp() {
        return destinationIp;
    }

    public IP getPreviousHopIp() {
        return previousHopIP;
    }

    public String getPacketTypeName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        //Calculate the sizes
        var headerSize = getHeaderSize(font);
        var contentSize = getContentSize(font);

        var size = new Vec2(MathUtil.max(headerSize.x, contentSize.x, 70), headerSize.y + contentSize.y + 2);

        // Background
        RenderUtil.renderBackgroundQuad(poseStack, buffer, size.x + 8, size.y, alpha);

        // Slight Z offset to avoid z-fighting
        poseStack.pushPose();
        poseStack.translate(0, 0, -0.01f);

        renderHeader(poseStack, buffer, packedLight, alpha, font, size.x);
        RenderUtil.renderHLine(alpha, size.x, headerSize.y, poseStack, buffer, packedLight);
        RenderUtil.renderHLine(alpha, size.x, headerSize.y + 0.5f, poseStack, buffer, packedLight);

        poseStack.translate(0, headerSize.y + 2, 0);

        renderPacketContent(poseStack, buffer, packedLight, alpha, font, size.x);

        poseStack.popPose();
    }

    protected Vec2 getContentSize(Font font) {
        return Vec2.ZERO;
    }


    protected void renderPacketContent(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float alpha, Font font, float width) {

    }

    protected Vec2 getHeaderSize(Font font) {
        String line = getClass().getSimpleName() + " " + id.toString().substring(0, 8);
        String line2 = sourceIp + " -> " + destinationIp;

        var width = MathUtil.max(font.width(line), font.width(line2));

        return new Vec2(width, font.lineHeight * 2).scale(0.5f);
    }

    protected void renderHeader(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float alpha, Font font, float width) {
        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);
        width *= 2;

        RenderUtil.drawString(RenderUtil.Align.LEFT, getClass().getSimpleName(), 0xAAFFAA, width, 0, poseStack, buffer, packedLight);
        RenderUtil.drawString(RenderUtil.Align.RIGHT, id.toString().substring(0, 8), 0xAAAAAA, width, 0, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.LEFT, sourceIp.toString(), alpha, width, 8, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.CENTER, "->", alpha, width, 8, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, destinationIp.toString(), alpha, width, 8, poseStack, buffer, packedLight);

        poseStack.popPose();
    }
}
