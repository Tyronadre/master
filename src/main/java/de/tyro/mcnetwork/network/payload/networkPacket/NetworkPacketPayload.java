package de.tyro.mcnetwork.network.payload.networkPacket;

import de.tyro.mcnetwork.simulation.packet.INetworkPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

public record NetworkPacketPayload(INetworkPacket packet) {
    public static StreamCodec<FriendlyByteBuf, NetworkPacketPayload> codec() {
        return new StreamCodec<>() {
            @Override
            public @NotNull NetworkPacketPayload decode(@NotNull FriendlyByteBuf buffer) {
                var packetClassNameSize = buffer.readInt();
                var packetClassName = buffer.readCharSequence(packetClassNameSize, Charset.defaultCharset());
                try {
                    Class<? extends INetworkPacket> clazz = (Class<? extends INetworkPacket>) Class.forName(packetClassName.toString());
                    return new NetworkPacketPayload(NetworkPacketCodecRegistry.codecOf(clazz).decode(buffer));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void encode(FriendlyByteBuf buffer, NetworkPacketPayload payload) {
                var className = payload.packet.getClass().getName();
                buffer.writeInt(className.length());
                buffer.writeCharSequence(className, Charset.defaultCharset());
                var packet = payload.packet();

                StreamCodec<FriendlyByteBuf, INetworkPacket> codec = (StreamCodec<FriendlyByteBuf, INetworkPacket>) NetworkPacketCodecRegistry.codecOf(packet.getClass());
                codec.encode(buffer, payload.packet);
            }
        };
    }
}

