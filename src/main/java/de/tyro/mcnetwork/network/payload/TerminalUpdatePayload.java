package de.tyro.mcnetwork.network.payload;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import de.tyro.mcnetwork.network.NetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record TerminalUpdatePayload(BlockPos pos, String input) implements CustomPacketPayload {
    public static final Type<TerminalUpdatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, "TerminalUpdatePayload"));
    public static final StreamCodec<FriendlyByteBuf, TerminalUpdatePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerminalUpdatePayload::pos,
            ByteBufCodecs.STRING_UTF8, TerminalUpdatePayload::input,
            TerminalUpdatePayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleServerbound(IPayloadContext context) {
        var level = context.player().level();
        var be = NetworkUtil.getBlockEntityAt(ComputerBlockEntity.class, level, pos);
        var terminal = be.getTerminal();

        terminal.notifyPlayers(this);
    }

    public void handleClientbound(IPayloadContext context) {
        var level = context.player().level();
        var be = NetworkUtil.getBlockEntityAt(ComputerBlockEntity.class, level, pos);
        var terminal = be.getTerminal();

        terminal.
    }
}
