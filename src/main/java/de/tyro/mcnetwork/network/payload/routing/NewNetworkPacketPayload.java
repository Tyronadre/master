package de.tyro.mcnetwork.network.payload.routing;

import com.mojang.logging.LogUtils;
import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import de.tyro.mcnetwork.network.NetworkUtil;
import de.tyro.mcnetwork.routing.packet.IApplicationPaket;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

public record NewNetworkPacketPayload(INetworkPacket packet, Integer ttl, BlockPos pos) implements CustomPacketPayload {
    static Logger logger = LogUtils.getLogger();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return getType();
    }

    public static Type<NewNetworkPacketPayload> getType() {
        return new Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, NewNetworkPacketPayload.class.getSimpleName().toLowerCase()));
    }

    public void handle(IPayloadContext context) {
        logger.debug("Receiving {} on {}", packet, context.player().level().isClientSide ? "client" : "server");

        var level = context.player().level();
        var be = NetworkUtil.getBlockEntityAt(ComputerBlockEntity.class, level, pos());
        if (level.isClientSide && packet.getNetworkFrame() == null) {
            be.onApplicationPacketReceived(((IApplicationPaket) packet));
        }
        be.getRoutingProtocol().send(packet, ttl);
    }

    public static final StreamCodec<FriendlyByteBuf, NewNetworkPacketPayload> STEAM_CODEC = StreamCodec.composite(
            NetworkPacketPayload.codec(), self -> new NetworkPacketPayload(self.packet),
            ByteBufCodecs.INT, NewNetworkPacketPayload::ttl,
            BlockPos.STREAM_CODEC, NewNetworkPacketPayload::pos,
            (NetworkPacketPayload payload, Integer ttl, BlockPos pos) -> new NewNetworkPacketPayload(payload.packet(), ttl, pos)
    );

}

