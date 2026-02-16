package de.tyro.mcnetwork.network.payload;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import de.tyro.mcnetwork.network.NetworkUtil;
import de.tyro.mcnetwork.routing.INetworkNode;
import it.unimi.dsi.fastutil.bytes.Byte2CharOpenCustomHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record TerminalWatchingPayload(BlockPos pos, boolean add) implements CustomPacketPayload {
    public static final Type<TerminalWatchingPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, TerminalWatchingPayload.class.getSimpleName().toLowerCase()));
    public static final StreamCodec<FriendlyByteBuf, TerminalWatchingPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerminalWatchingPayload::pos,
            ByteBufCodecs.BOOL, TerminalWatchingPayload::add,
            TerminalWatchingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleServerbound(IPayloadContext context) {
        var serverplayer = (ServerPlayer) context.player();
        var be = NetworkUtil.getBlockEntityAt(ComputerBlockEntity.class, serverplayer.level(), pos);
        var terminal = be.getTerminal();

        if (add) terminal.registerPlayer(serverplayer);
        else terminal.unregisterPlayer(serverplayer);
    }


}
