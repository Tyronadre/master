package de.tyro.mcnetwork.network.payload.networkPacket;

import de.tyro.mcnetwork.network.BetterByteBuf;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.UUID;

public class NetworkPacketCodecRegistry {

    private static final HashMap<Class<?>, NetworkPacketCodec<?>> register = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends INetworkPacket> StreamCodec<FriendlyByteBuf, T> codecOf(Class<T> clazz) {
        if (!register.containsKey(clazz)) throw new IllegalArgumentException("Class " + clazz + " is not registered!");
        return (StreamCodec<FriendlyByteBuf, T>) register.get(clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T extends INetworkPacket> NetworkPacketCodec<T> handlerOf(Class<T> clazz) {
        if (!register.containsKey(clazz)) throw new IllegalArgumentException("Class " + clazz + " is not registered!");
        return (NetworkPacketCodec<T>) register.get(clazz);
    }

    public static <T extends INetworkPacket> void register(Class<T> clazz, NetworkPacketCodec<T> codec) {
        register.put(clazz, codec);
    }

    public static <T extends INetworkPacket> void register(Class<T> clazz, NetworkPacketCodec.Decoder<T> decoder, NetworkPacketCodec.Encoder<T> encoder) {
        register.put(clazz, new NetworkPacketCodec<T>() {
            @Override
            public T decodeActual(UUID uuid, IP originatorIP, IP destinationIP, BetterByteBuf buf) {
                return decoder.decode(buf, uuid, originatorIP, destinationIP);
            }

            @Override
            public void encodeActual(BetterByteBuf buffer, T packet) {
                encoder.encode(buffer, packet);
            }
        });
    }

    public static <T extends INetworkPacket> void register(Class<T> clazz, NetworkPacketCodec.Decoder<T> decoder, NetworkPacketCodec.Encoder<T> encoder, NetworkPacketCodec.Handler<T> handler) {
        register.put(clazz, new NetworkPacketCodec<T>() {
            @Override
            protected T decodeActual(UUID uuid, IP originatorIP, IP destinationIP, BetterByteBuf buf) {
                return decoder.decode(buf, uuid, originatorIP, destinationIP);
            }

            @Override
            protected void encodeActual(BetterByteBuf buffer, T packet) {
                encoder.encode(buffer, packet);
            }

            @Override
            protected void handleActual(T packet, IPayloadContext context) {
                handler.handle(packet, context);
            }
        });
    }

    public static <T extends INetworkPacket> boolean isRegistered(Class<T> clazz) {
        return register.containsKey(clazz);
    }
}
