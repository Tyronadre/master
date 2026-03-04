package de.tyro.mcnetwork.network;

import de.tyro.mcnetwork.network.payload.networkPacket.NetworkPacketCodec;
import de.tyro.mcnetwork.network.payload.networkPacket.NetworkPacketCodecRegistry;
import de.tyro.mcnetwork.network.payload.networkPacket.NetworkPacketPayload;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamEncoder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class BetterByteBuf extends FriendlyByteBuf {

    public BetterByteBuf(ByteBuf source) {
        super(source);
    }

    public BetterByteBuf writeIP(IP ip) {
        writeIP(this, ip);
        return this;
    }

    private static void writeIP(FriendlyByteBuf buf, IP ip) {
        for (int i = 0; i < 4; i++) buf.writeByte(ip.asArray()[i]);
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
        writeCollection(ipList, BetterByteBuf::writeIP);
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
