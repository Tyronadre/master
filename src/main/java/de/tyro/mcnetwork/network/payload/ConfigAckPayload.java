package de.tyro.mcnetwork.network.payload;

import de.tyro.mcnetwork.MCNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ConfigAckPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConfigAckPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, "ack"));
    public static final StreamCodec<ByteBuf, ConfigAckPayload> STREAM_CODEC = StreamCodec.unit(new ConfigAckPayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
    }
}
