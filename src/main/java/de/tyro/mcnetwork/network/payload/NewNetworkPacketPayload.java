package de.tyro.mcnetwork.network.payload;

import com.mojang.logging.LogUtils;
import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import de.tyro.mcnetwork.network.NetworkUtil;
import de.tyro.mcnetwork.network.payload.networkPacket.NetworkPacketCodecRegistry;
import de.tyro.mcnetwork.network.payload.networkPacket.NetworkPacketPayload;
import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.packet.IApplicationPacket;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

public record NewNetworkPacketPayload(INetworkPacket packet, Integer ttl, BlockPos pos, Boolean sendToSelf) implements CustomPacketPayload {
    static Logger logger = LogUtils.getLogger();

    /**
     * Sends this networkPacket to the same node
     *
     * @param packet the packet
     * @param node the node
     */
    public static void sendToSelf(INetworkPacket packet, INetworkNode node) {
        PacketDistributor.sendToAllPlayers( new NewNetworkPacketPayload(packet, -1,node.getBlockPos(), true));
    }

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
        if (be == null) {
            logger.error("Could not find computer block at {}", pos);
            return;
        }

        NetworkPacketCodecRegistry.handlerOf(packet.getClass()).handle(packet, context.flow().isClientbound());

        if (sendToSelf) {
            if (packet instanceof IProtocolPaket p) {
                be.getRoutingProtocol().onProtocolPacketReceived(p);
            } else if (packet instanceof IApplicationPacket p) {
                be.onApplicationPacketReceived(p);
            }
        } else {
            be.getRoutingProtocol().send(packet, ttl);
        }
    }

    public static final StreamCodec<FriendlyByteBuf, NewNetworkPacketPayload> STEAM_CODEC = StreamCodec.composite(
            NetworkPacketPayload.codec(), it -> new NetworkPacketPayload(it.packet),
            ByteBufCodecs.INT, NewNetworkPacketPayload::ttl,
            BlockPos.STREAM_CODEC, NewNetworkPacketPayload::pos,
            ByteBufCodecs.BOOL, NewNetworkPacketPayload::sendToSelf,
            (payload, ttl, pos, self) -> new NewNetworkPacketPayload(payload.packet(), ttl, pos, self)
    );

}

