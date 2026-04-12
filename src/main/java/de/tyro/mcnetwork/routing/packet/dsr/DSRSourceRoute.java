package de.tyro.mcnetwork.routing.packet.dsr;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DSRSourceRoute extends NetworkPacket implements IProtocolPaket {
    boolean firstHopExternalFlag;
    boolean lastHopExternalFlag;
    int segLeft;
    final List<IP> addresses = new ArrayList<>();
    INetworkPacket packet;

    public DSRSourceRoute(IP originatorIP, IP destinationIP, List<IP> addresses, boolean firstHopExternalFlag, boolean lastHopExternalFlag, INetworkPacket packet) {
        super(originatorIP, destinationIP);
        this.addresses.addAll(addresses);
        segLeft = addresses.size() - 1;
        this.firstHopExternalFlag = firstHopExternalFlag;
        this.lastHopExternalFlag = lastHopExternalFlag;
        this.packet = packet;
    }

    public DSRSourceRoute(UUID uuid, IP originatorIP, IP destinationIP, boolean firstHopExternalFlag, boolean lastHopExternalFlag, int segLeft, List<IP> ips, INetworkPacket iNetworkPacket) {
        super(uuid, originatorIP, destinationIP);
        this.addresses.addAll(ips);
        this.segLeft = segLeft;
        this.firstHopExternalFlag = firstHopExternalFlag;
        this.lastHopExternalFlag = lastHopExternalFlag;
        this.packet = iNetworkPacket;
    }

    @Override
    public INetworkPacket copy() {
        return new DSRSourceRoute(getId(), getOriginatorIP(), getDestinationIP(), firstHopExternalFlag, lastHopExternalFlag, segLeft, addresses, packet);
    }

    public IP getNextAddress() {
        if (segLeft == 0) return null;
        var address = addresses.get(addresses.size() - segLeft);
        segLeft--;
        return address;
    }


    @Override
    protected void renderPacketContent(RenderUtil renderer, PoseStack poseStack, float width) {
        var pose = renderer.getPoseStack();
        pose.pushPose();

        pose.scale(0.5f, 0.5f, 0.5f);
        width *= 2;

        renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, "Route: ", width, 0);

        int y = 0;
        for (IP address : addresses) {
            renderer.drawStringWithAlphaColor(RenderUtil.Align.RIGHT, address.toString(), width, y);
            y += 10;
        }
        renderer.renderHLineWithAlphaColor(width, y);
        y += 7;

        pose.translate(0,y,0);
        pose.scale(2f,2f,2f);

        packet.render(renderer);
        pose.popPose();
    }

    @Override
    public Vec2 getRenderSize(Font font) {
        var innerSize = packet.getRenderSize(font);

        return new Vec2(Math.max(100, innerSize.x), 23 + innerSize.y + getHeaderSize(font).y);
    }

    public List<IP> getAddresses() {
        return addresses;
    }

    public boolean getFirstHopExternalFlag() {
        return firstHopExternalFlag;
    }

    public boolean getLastHopExternalFlag() {
        return lastHopExternalFlag;
    }

    public int getSegLeft() {
        return segLeft;
    }

    public INetworkPacket getPacket() {
        return packet;
    }
}
