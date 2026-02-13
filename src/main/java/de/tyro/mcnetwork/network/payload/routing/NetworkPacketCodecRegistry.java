package de.tyro.mcnetwork.network.payload.routing;

import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.UUID;

public class NetworkPacketCodecRegistry {

    private static final HashMap<Class<?>, NetworkPacketCodec<?>> register = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends INetworkPacket> StreamCodec<FriendlyByteBuf, T> codecOf(Class<T> clazz) {

        if (!register.containsKey(clazz)) throw new IllegalArgumentException("Class " + clazz + " is not registered!");
        return (StreamCodec<FriendlyByteBuf, T>) register.get(clazz);
    }

    public static <T extends INetworkPacket> void register(Class<T> clazz, NetworkPacketCodec<T> codec) {
        register.put(clazz, codec);
    }

    public static <T extends INetworkPacket> void register(Class<T> clazz, NetworkPacketCodec.Decoder<T> decoder, NetworkPacketCodec.Encoder<T> encoder) {
        register.put(clazz, new NetworkPacketCodec<T>() {
            @Override
            public T decodeActual(UUID uuid, IP originatorIP, IP destinationIP, FriendlyByteBuf buf) {
                return decoder.decode(buf, uuid, originatorIP, destinationIP);
            }

            @Override
            public void encodeActual(FriendlyByteBuf buffer, T packet) {
                encoder.encode(buffer, packet);
            }
        });
    }

    public static <T extends INetworkPacket> boolean isRegistered(Class<T> clazz) {
        return register.containsKey(clazz);
    }
}
