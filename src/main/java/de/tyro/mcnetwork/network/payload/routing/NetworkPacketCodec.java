package de.tyro.mcnetwork.network.payload.routing;

import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class NetworkPacketCodec<T extends INetworkPacket> implements StreamCodec<FriendlyByteBuf, T> {
    public abstract T decodeActual(UUID uuid, IP originatorIP, IP destinationIP, FriendlyByteBuf buf);

    public abstract void encodeActual(FriendlyByteBuf buffer, T packet);

    @Override
    public @NotNull T decode(@NotNull FriendlyByteBuf buffer) {
        return decodeActual(buffer.readUUID(), readIP(buffer), readIP(buffer), buffer);
    }

    @Override
    public void encode(FriendlyByteBuf buffer, T networkPacket) {
        buffer.writeUUID(networkPacket.getId());
        writeIP(buffer, networkPacket.getOriginatorIP());
        writeIP(buffer, networkPacket.getDestinationIP());
        encodeActual(buffer, networkPacket);
    }

    protected void writeIP(FriendlyByteBuf buffer, IP ip) {
        for (int i = 0; i < 4; i++) buffer.writeByte(ip.asArray()[i]);
    }

    protected IP readIP(ByteBuf buffer) {
        int[] ip = new int[4];
        for (int i = 0; i < 4; i++) ip[i] = buffer.readByte();
        return new IP(ip);
    }

    @FunctionalInterface
    public interface Decoder<T extends INetworkPacket> {
        T decode(FriendlyByteBuf buf, UUID uuid, IP originatorIP, IP destinationIP);
    }

    @FunctionalInterface
    public interface Encoder<T extends INetworkPacket> {
        void encode(FriendlyByteBuf buffer, T packet);
    }
}
