package de.tyro.mcnetwork.network.payload.networkPacket;

import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public abstract class NetworkPacketHandler<T extends INetworkPacket> {

    public void handle(T packet, IPayloadContext context) {

        if (context.flow().isClientbound()) {
            handleClientbound(packet, context);
        } else {
            handleServerbound(packet, context);
        }

    }

    abstract void handleClientbound(T packet, IPayloadContext context);

    abstract void handleServerbound(T packet, IPayloadContext context);

}
