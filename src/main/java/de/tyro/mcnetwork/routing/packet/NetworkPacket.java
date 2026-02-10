package de.tyro.mcnetwork.routing.packet;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.MathUtil;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.NetworkFrame;
import de.tyro.mcnetwork.routing.IP;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class NetworkPacket implements INetworkPacket {
    public final UUID id = UUID.randomUUID();
    IP originatorIP;
    IP destinationIP;
    private NetworkFrame frame;

    protected NetworkPacket(IP originatorIP, IP destinationIP) {
        this.originatorIP = originatorIP;
        this.destinationIP = destinationIP;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public IP getOriginatorIP() {
        return originatorIP;
    }

    @Override
    public IP getDestinationIP() {
        return destinationIP;
    }

    @Override
    public NetworkFrame getNetworkFrame() {
        return frame;
    }

    @Override
    public void setFrame(NetworkFrame frame) {
        this.frame = frame;
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

        renderHeader(poseStack, buffer, packedLight, alpha, size.x);
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
        String line2 = originatorIP + " -> " + destinationIP;

        var width = MathUtil.max(font.width(line), font.width(line2));

        return new Vec2(width, font.lineHeight * 2).scale(0.5f);
    }

    protected void renderHeader(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float alpha, float width) {
        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);
        width *= 2;

        RenderUtil.drawString(RenderUtil.Align.LEFT, getClass().getSimpleName(), 0xAAFFAA, width, 0, poseStack, buffer, packedLight);
        RenderUtil.drawString(RenderUtil.Align.RIGHT, id.toString().substring(0, 8), 0xAAAAAA, width, 0, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.LEFT, originatorIP == null ? "?" : originatorIP.toString(), alpha, width, 8, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.CENTER, "->", alpha, width, 8, poseStack, buffer, packedLight);
        RenderUtil.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, destinationIP == null ? "?" : destinationIP.toString(), alpha, width, 8, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    public void setDestinationIP(IP destination) {
        this.destinationIP = destination;
    }

    @Override
    public @NotNull Type<? extends INetworkPacket> type() {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, getClass().getSimpleName()));
    }

    @Override
    public StreamCodec<ByteBuf, ? extends INetworkPacket> getStreamCodec() {
        return null;
    }
}
