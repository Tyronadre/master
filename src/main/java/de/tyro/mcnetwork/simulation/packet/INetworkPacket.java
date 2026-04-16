package de.tyro.mcnetwork.simulation.packet;

import de.tyro.mcnetwork.entity.NetworkFrameEntity;
import de.tyro.mcnetwork.simulation.IHudRenderer;
import de.tyro.mcnetwork.simulation.IP;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public interface INetworkPacket extends IHudRenderer, CustomPacketPayload {
    UUID getId();
    IP getDestinationIP();
    IP getOriginatorIP();
    INetworkPacket copy();

    void setFrame(NetworkFrameEntity entity);
    NetworkFrameEntity getNetworkFrame();
}
