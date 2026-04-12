package de.tyro.mcnetwork.network.payload;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import de.tyro.mcnetwork.network.NetworkUtil;
import de.tyro.mcnetwork.routing.protocol.AODVProtocol;
import de.tyro.mcnetwork.routing.protocol.DSRProtocol;
import de.tyro.mcnetwork.routing.protocol.LARProtocol;
import de.tyro.mcnetwork.routing.protocol.OLSRProtocol;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record RoutingProtocolSettingsPayload(String routingProtocol, BlockPos pos) implements CustomPacketPayload {

    public static Type<RoutingProtocolSettingsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, RoutingProtocolSettingsPayload.class.getSimpleName().toLowerCase()));
    public static StreamCodec<FriendlyByteBuf, RoutingProtocolSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RoutingProtocolSettingsPayload::routingProtocol,
            BlockPos.STREAM_CODEC, RoutingProtocolSettingsPayload::pos,
            RoutingProtocolSettingsPayload::new
    );

    public void handle(IPayloadContext context) {
        var level = context.player().level();
        var be = NetworkUtil.getBlockEntityAt(ComputerBlockEntity.class, level, pos());
        if (be == null) return;

        if (context.flow().isServerbound()) PacketDistributor.sendToAllPlayers(this);

        be.setProtocol(switch (routingProtocol){
            case "AODVProtocol" -> new AODVProtocol(be);
            case "DSRProtocol" -> new DSRProtocol(be);
            case "LARProtocol" -> new LARProtocol(be);
            case "OLSRProtocol" -> new OLSRProtocol(be);
            default -> throw new IllegalArgumentException("Unknown routing protocol " +  routingProtocol);
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
