package de.tyro.mcnetwork.routing.packet.dsr;

import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;

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
        return null;
    }

    public IP getNextAddress() {
        if (segLeft == 0) return null;
        var address = addresses.get(addresses.size() - segLeft);
        segLeft--;
        return address;
    }

    @Override
    public void render(RenderUtil renderer) {
        int y = 0;
        for (IP address : addresses) {
            renderer.drawStringWithAlphaColor(RenderUtil.Align.LEFT, address.toString(), 200, y);
            y += 10;
        }
        renderer.renderHLineWithAlphaColor(200, y);
        y += 5;

        packet.render(renderer);
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
