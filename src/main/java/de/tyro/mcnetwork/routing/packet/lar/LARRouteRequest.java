package de.tyro.mcnetwork.routing.packet.lar;

import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class LARRouteRequest extends NetworkPacket implements IProtocolPaket {
    public final Vec3 sourcePos;
    public final Vec3 destExpectedPos;
    public final int requestZoneSize; // e.g., radius or width
    public int hopCount;

    public LARRouteRequest(IP sourceIp, IP destinationIp, Vec3 sourcePos, Vec3 destExpectedPos, int requestZoneSize) {
        super(sourceIp, destinationIp);
        this.sourcePos = sourcePos;
        this.destExpectedPos = destExpectedPos;
        this.requestZoneSize = requestZoneSize;
        this.hopCount = 0;
    }

    public LARRouteRequest(UUID uuid, IP sourceIp, IP destinationIp, Vec3 sourcePos, Vec3 destExpectedPos, int requestZoneSize, int hopCount) {
        super(uuid, sourceIp, destinationIp);
        this.sourcePos = sourcePos;
        this.destExpectedPos = destExpectedPos;
        this.requestZoneSize = requestZoneSize;
        this.hopCount = hopCount;
    }

    public INetworkPacket copy() {
        return new LARRouteRequest(getOriginatorIP(), getDestinationIP(), sourcePos, destExpectedPos, requestZoneSize);
    }
}
