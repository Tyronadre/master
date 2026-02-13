package de.tyro.mcnetwork.network.payload.routing;

import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

public class NetworkPacketPayloadUtil {

    @SuppressWarnings("unchecked")
    public static <V extends INetworkPacket> V decode(@NotNull RegistryFriendlyByteBuf buffer) {
        var packetClassNameSize = buffer.readInt();
        var packetClassName = buffer.readCharSequence(packetClassNameSize, Charset.defaultCharset());
        try {
            var clazz = (Class<V>) Class.forName(packetClassName.toString());
            return NetworkPacketCodecRegistry.codecOf(clazz).decode(buffer);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
