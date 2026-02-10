package de.tyro.mcnetwork.routing;

import com.mojang.datafixers.util.Function4;
import de.tyro.mcnetwork.MCNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

public class IP implements CustomPacketPayload {
    public static final IP ZERO = new IP(new int[]{0, 0, 0, 0});
    public static final IP BROADCAST = new IP(new int[]{255, 255, 255, 255});
    int[] address;

    public IP(int[] address) {
        if (address == null || address.length != 4) throw new IllegalArgumentException("Invalid IP address: " + Arrays.toString(address));
        this.address = address;
    }

    public IP(String address) {
        if (address == null) throw new IllegalArgumentException("Invalid IP address: null");
        var split = address.split("\\.");
        if (split.length != 4) throw new IllegalArgumentException("Invalid IP address: " + address);

        this.address = new int[4];
        for (int i = 0; i < split.length; i++) this.address[i] = Integer.parseInt(split[i]);
    }

    public static boolean validateIp(String ip) {
        return ip.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    }

    public static IP getNextFreeIP() {
        int[] ip = new int[4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 255; j++) {
                ip[3 - i] = j;
                var ipO = new IP(ip);

                if (ipO.equals(ZERO) || ipO.equals(BROADCAST)) continue;
                if (SimulationEngine.INSTANCE.getNodeList().stream().noneMatch(it -> it.getIP().equals(ipO))) return ipO;
            }
        }
        throw new IllegalStateException("No free IP address found!");
    }

    @Override
    public String toString() {
        return Arrays.stream(address).mapToObj(String::valueOf).collect(Collectors.joining("."));
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof IP ip)) return false;

        return Arrays.equals(address, ip.address);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(address);
    }

    public static final CustomPacketPayload.Type<IP> TYPE = new CustomPacketPayload.Type<IP>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, "ip"));

    public static final StreamCodec<ByteBuf, IP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, (IP ip) -> ip.address[0],
            ByteBufCodecs.INT, (IP ip) -> ip.address[1],
            ByteBufCodecs.INT, (IP ip) -> ip.address[2],
            ByteBufCodecs.INT, (IP ip) -> ip.address[3],
            (ip0, ip1, ip2, ip3) -> new IP(new int[]{ip0, ip1, ip2, ip3})
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
