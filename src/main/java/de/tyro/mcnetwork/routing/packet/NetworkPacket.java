package de.tyro.mcnetwork.routing.packet;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.util.MathUtil;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.entity.NetworkFrameEntity;
import de.tyro.mcnetwork.routing.IP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class NetworkPacket implements INetworkPacket {
    public UUID id;
    IP originatorIP;
    IP destinationIP;
    private NetworkFrameEntity frame;

    protected NetworkPacket(IP originatorIP, IP destinationIP) {
        this.id = UUID.randomUUID();
        this.originatorIP = originatorIP;
        this.destinationIP = destinationIP;
    }

    public NetworkPacket(UUID uuid, IP originatorIP, IP destinationIP) {
        this.id = uuid;
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
    public NetworkFrameEntity getNetworkFrame() {
        return frame;
    }

    @Override
    public void setFrame(NetworkFrameEntity frame) {
        this.frame = frame;
    }

    @Override
    public void render(RenderUtil renderer) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        var poseStack = renderer.getPoseStack();

        //Calculate the sizes
        var headerSize = getHeaderSize(font);
        var contentSize = getRenderSize(font);

        var size = new Vec2(MathUtil.max(headerSize.x, contentSize.x, 70), headerSize.y + contentSize.y + 2);

        // Background
        renderer.renderBackgroundQuad(size.x + 8, size.y);

        // Slight Z offset to avoid z-fighting
        poseStack.pushPose();
        poseStack.translate(0, 0, -0.01f);

        renderHeader(renderer, poseStack, size.x);
        renderer.renderHLineWithAlphaColor(size.x, headerSize.y);
        renderer.renderHLineWithAlphaColor(size.x, headerSize.y + 0.5f);

        poseStack.translate(0, headerSize.y + 2, 0);

        renderPacketContent(renderer, poseStack,  size.x);

        poseStack.popPose();
    }

    public Vec2 getRenderSize(Font font) {
        return Vec2.ZERO;
    }

    protected void renderPacketContent(RenderUtil renderer, PoseStack poseStack, float width) {

    }

    protected Vec2 getHeaderSize(Font font) {
        String line = getClass().getSimpleName() + " " + id.toString().substring(0, 8);
        String line2 = originatorIP + " -> " + destinationIP;

        var width = Math.max(font.width(line), font.width(line2));

        return new Vec2(width, font.lineHeight * 2).scale(0.5f);
    }

    protected void renderHeader(RenderUtil renderer, PoseStack poseStack, float width) {
        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);
        width *= 2;

        renderer.drawString(RenderUtil.Align.LEFT, getClass().getSimpleName(), 0xAAFFAA, width, 0);
        renderer.drawString(RenderUtil.Align.RIGHT, id.toString().substring(0, 8), 0xAAAAAA, width, 0);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, originatorIP == null ? "?" : originatorIP.toString(), width, 8);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.CENTER, "->", width, 8);
        renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, destinationIP == null ? "?" : destinationIP.toString(), width, 8);

        poseStack.popPose();
    }

    public void setDestinationIP(IP destination) {
        this.destinationIP = destination;
    }

    public void setOriginatorIP(IP originatorIP) {
        this.originatorIP = originatorIP;
    }


    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends INetworkPacket> type() {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, getClass().getSimpleName().toLowerCase()));
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Long.toHexString(getId().getLeastSignificantBits());
    }
}


