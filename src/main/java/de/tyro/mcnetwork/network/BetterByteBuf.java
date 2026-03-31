package de.tyro.mcnetwork.network;

import de.tyro.mcnetwork.network.payload.networkPacket.NetworkPacketPayload;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class BetterByteBuf extends FriendlyByteBuf {
    public static StreamCodec<FriendlyByteBuf, IP> IP_STREAM_CODEC = new IPStreamCodec();

    private static class IPStreamCodec implements StreamCodec<FriendlyByteBuf, IP> {

        @Override
        public @NotNull IP decode(@NotNull FriendlyByteBuf buffer) {
            int[] ip = new int[4];
            for (int i = 0; i < 4; i++) ip[i] = 0xFF & buffer.readByte();
            return new IP(ip);
        }

        @Override
        public void encode(@NotNull FriendlyByteBuf buffer, @NotNull IP value) {
            for (int i = 0; i < 4; i++) buffer.writeByte(value.asArray()[i]);

        }
    }


    public BetterByteBuf(ByteBuf source) {
        super(source);
    }

    public BetterByteBuf writeIP(IP ip) {
        IP_STREAM_CODEC.encode(this, ip);
        return this;
    }

    public BetterByteBuf writeVec3R(Vec3 vec3) {
        super.writeVec3(vec3);
        return this;
    }


    public IP readIP() {
        return readIP(this);
    }

    private static IP readIP(FriendlyByteBuf buf) {

        int[] ip = new int[4];
        for (int i = 0; i < 4; i++) ip[i] = 0xFF & buf.readByte();
        return new IP(ip);
    }

    @Override
    public @NotNull BetterByteBuf writeUUID(@NotNull UUID uuid) {
        super.writeUUID(uuid);
        return this;
    }

    @Override
    public BetterByteBuf writeInt(int value) {
        super.writeInt(value);
        return this;
    }

    public BetterByteBuf writeIPList(List<IP> ipList) {
        writeCollection(ipList, (buffer, value) -> IP_STREAM_CODEC.encode(buffer, value));
        return this;
    }


    public List<IP> readIPList() {
        return readList(BetterByteBuf::readIP);
    }

    @Override
    public @NotNull BetterByteBuf writeBoolean(boolean value) {
        super.writeBoolean(value);
        return this;
    }

    public BetterByteBuf writePacket(INetworkPacket packet) {
        var codec = NetworkPacketPayload.codec();
        var payload = new NetworkPacketPayload(packet);
        codec.encode(this, payload);
        return this;
    }

    public INetworkPacket readPacket() {
        var codec = NetworkPacketPayload.codec();
        var payload = codec.decode(this);
        return payload.packet();
    }

}
