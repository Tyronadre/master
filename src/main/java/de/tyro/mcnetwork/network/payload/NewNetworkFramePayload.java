package de.tyro.mcnetwork.network.payload;

import com.mojang.logging.LogUtils;
import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import de.tyro.mcnetwork.entity.NetworkFrameEntity;
import de.tyro.mcnetwork.network.NetworkUtil;
import de.tyro.mcnetwork.network.payload.networkPacket.NetworkPacketPayload;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

public record NewNetworkFramePayload(BlockPos from, BlockPos to, INetworkPacket packet, int ttl) implements CustomPacketPayload {
    static Logger logger = LogUtils.getLogger();

    private NewNetworkFramePayload(BlockPos from, BlockPos to, NetworkPacketPayload payload, int ttl) {
        this(from, to, payload.packet(), ttl);
    }

    public NewNetworkFramePayload(NetworkFrameEntity networkFrame) {
        this(networkFrame.getFrom().getBlockPos(), networkFrame.getTo().getBlockPos(), networkFrame.getPacket(), networkFrame.getTtl());
    }

    public static Type<NewNetworkFramePayload> getType() {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, NetworkPacketPayload.class.getSimpleName().toLowerCase()));
    }

    public static CustomPacketPayload toSelf(NetworkFrameEntity frame) {
        return new NewNetworkFramePayload(frame.getFrom().getBlockPos(), frame.getTo().getBlockPos(), frame.getPacket(), -1);
    }

    private NetworkPacketPayload payload() {
        return new NetworkPacketPayload(packet);
    }

    public static final StreamCodec<FriendlyByteBuf, NewNetworkFramePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NewNetworkFramePayload::from,
            BlockPos.STREAM_CODEC, NewNetworkFramePayload::to,
            NetworkPacketPayload.codec(), NewNetworkFramePayload::payload,
            ByteBufCodecs.INT, NewNetworkFramePayload::ttl,
            NewNetworkFramePayload::new
    );


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return getType();
    }


    public void handle(IPayloadContext context) {
        logger.debug("Received {} on {}", this, context.player().level().isClientSide ? "client" : "server");


        var level = context.player().level();
        var from = NetworkUtil.getBlockEntityAt(ComputerBlockEntity.class, level, from());
        var to = NetworkUtil.getBlockEntityAt(ComputerBlockEntity.class, level, to());


        var networkFrameEntity = new NetworkFrameEntity(level, from, to, ttl(), packet());


        if (level.isClientSide && ttl == -1) {
            to.onFrameReceive(networkFrameEntity);
        } else {
            context.player().level().addFreshEntity(networkFrameEntity);
        }
    }
}
