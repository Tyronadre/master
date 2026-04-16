package de.tyro.mcnetwork.simulation.packet.lar;

import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.packet.INetworkPacket;
import de.tyro.mcnetwork.simulation.packet.IProtocolPaket;
import de.tyro.mcnetwork.simulation.packet.NetworkPacket;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class LARRouteReply extends NetworkPacket implements IProtocolPaket {
    public final Vec3 sourcePos;
    public final Vec3 destPos;

    public LARRouteReply(IP sourceIp, IP destinationIp, Vec3 sourcePos, Vec3 destPos) {
        super(sourceIp, destinationIp);
        this.sourcePos = sourcePos;
        this.destPos = destPos;
    }

    public LARRouteReply(UUID uuid, IP sourceIp, IP destinationIp, Vec3 sourcePos, Vec3 destPos) {
        super(uuid, sourceIp, destinationIp);
        this.sourcePos = sourcePos;
        this.destPos = destPos;
    }

    public INetworkPacket copy() {
        return new LARRouteReply(getOriginatorIP(), getDestinationIP(), sourcePos, destPos);
    }
}
