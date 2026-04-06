package de.tyro.mcnetwork.network.payload.networkPacket;

import de.tyro.mcnetwork.network.BetterByteBuf;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class NetworkPacketCodec<T extends INetworkPacket> implements StreamCodec<FriendlyByteBuf, T> {
    protected abstract T decodeActual(UUID uuid, IP originatorIP, IP destinationIP, BetterByteBuf buf);

    protected abstract void encodeActual(BetterByteBuf buffer, T packet);

    @Override
    public @NotNull T decode(@NotNull FriendlyByteBuf buffer) {
        var buf = new BetterByteBuf(buffer);

        return decodeActual(buf.readUUID(), buf.readIP(), buf.readIP(), buf);
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buffer, T networkPacket) {
        var buf = new BetterByteBuf(buffer);

        buf.writeUUID(networkPacket.getId())
                .writeIP(networkPacket.getOriginatorIP() == null ? IP.ZERO : networkPacket.getOriginatorIP())
                .writeIP(networkPacket.getDestinationIP() == null ? IP.ZERO : networkPacket.getDestinationIP());
        encodeActual(buf, networkPacket);
    }

    @SuppressWarnings("unchecked")
    public void handle(INetworkPacket packet, Boolean onClientSide) {
        handleActual((T) packet, onClientSide);
    }

    protected void handleActual(T packet, Boolean onClientSide) {
    }


    @FunctionalInterface
    public interface Decoder<T extends INetworkPacket> {
        T decode(BetterByteBuf buf, UUID uuid, IP originatorIP, IP destinationIP);
    }

    @FunctionalInterface
    public interface Encoder<T extends INetworkPacket> {
        void encode(BetterByteBuf buffer, T packet);
    }

    @FunctionalInterface
    public interface Handler<T extends INetworkPacket> {
        void handle(T packet, Boolean onClientSide);
    }

}
