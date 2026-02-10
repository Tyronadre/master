package de.tyro.mcnetwork.routing.packet;

import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.NetworkFrame;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public interface INetworkPacket extends IPacketRenderable, CustomPacketPayload {
    UUID getId();
    IP getDestinationIP();
    IP getOriginatorIP();
    INetworkPacket copy();

    void setFrame(NetworkFrame inFlightPacket);
    NetworkFrame getNetworkFrame();

    StreamCodec<ByteBuf, ? extends INetworkPacket> getStreamCodec();
}
