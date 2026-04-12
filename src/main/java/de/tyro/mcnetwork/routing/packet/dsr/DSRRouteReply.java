package de.tyro.mcnetwork.routing.packet.dsr;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.client.RenderUtil;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DSRRouteReply extends NetworkPacket implements IProtocolPaket {
    // Set to indicate that the last hop given by the Route Reply (the
    // link from Address[n-1] to Address[n]) is actually an arbitrary
    // path in a network external to the DSR network; the exact route
    // outside the DSR network is not represented in the Route Reply.
    // Nodes caching this hop in their Route Cache MUST flag the
    // cached hop with the External flag.  Such hops MUST NOT be
    // returned in a cached Route Reply generated from this Route
    // Cache entry, and selection of routes from the Route Cache to
    // route a packet being sent SHOULD prefer routes that contain no
    // hops flagged as External.
    boolean lastHopExternalFlag = false;

    //  The source route being returned by the Route Reply.  The route
    // indicates a sequence of hops, originating at the source node
    // specified in the Destination Address field of the IP header of
    // the packet carrying the Route Reply, through each of the
    // Address[i] nodes in the order listed in the Route Reply, ending
    // at the node indicated by Address[n].  The number of addresses
    // present in the Address[1..n] field is indicated by the Opt Data
    // Len field in the option (n = (Opt Data Len - 1) / 4).
    List<IP> addresses;

    // Source Address
    //
    //    Set to the address of the node sending the Route Reply.  In the
    //    case of a node sending a reply from its Route Cache (Section
    //    3.3.2) or sending a gratuitous Route Reply (Section 3.4.3),
    //    this address can differ from the address that was the target of
    //    the Route Discovery.
    //
    // Destination Address
    //
    //    MUST be set to the address of the source node of the route
    //    being returned.  Copied from the Source Address field of the
    //    Route Request generating the Route Reply or, in the case of a
    //    gratuitous Route Reply, copied from the Source Address field of
    //    the data packet triggering the gratuitous Reply.
    public DSRRouteReply(IP sourceAddress, IP destinationAddress) {
        super(sourceAddress, destinationAddress);
    }

    public DSRRouteReply(UUID uuid, IP originatorIP, IP destinationIP, boolean lastHopExternalFlag, List<IP> addresses) {
        super(uuid, originatorIP, destinationIP);
        this.lastHopExternalFlag = lastHopExternalFlag;
        this.addresses = addresses;
    }

    @Override
    public INetworkPacket copy() {
        return new DSRRouteReply(getId(), getOriginatorIP(), getDestinationIP(), lastHopExternalFlag, addresses);
    }

    public boolean getLastHopExternalFlag() {
        return lastHopExternalFlag;
    }

    public void setLastHopExternal(boolean lastHopExternalFlag) {
        this.lastHopExternalFlag = lastHopExternalFlag;
    }

    public void setIdentifificationValue(Integer identificationValue) {
    }

    public void setAddresses(List<IP> addresses) {
        this.addresses = addresses;
    }

    public List<IP> getAddresses() {
        return addresses;
    }

    public List<IP> getRouteToHere(IP ip) {
        var list = new ArrayList<IP>();

        for (var address : addresses) {
            if (address.equals(ip)) break;
            list.add(address);
        }

        if (getDestinationIP().equals(ip)) list.add(ip);

        return list;
    }

    public IP getNextAddress(IP currentNodeAddress) {
        var index = addresses.indexOf(currentNodeAddress);
        if (index == addresses.size() - 1) return getDestinationIP();
        else return addresses.get(index + 1);
    }

    @Override
    protected void renderPacketContent(RenderUtil renderer, PoseStack poseStack, float width) {
        var y = 0;

        renderer.drawCollectionAsString(RenderUtil.Align.RIGHT, addresses, 100, renderer.getTextColorFromAlpha(), width, 0);
    }

    @Override
    public Vec2 getRenderSize(Font font) {
        return super.getRenderSize(font);
    }
}
