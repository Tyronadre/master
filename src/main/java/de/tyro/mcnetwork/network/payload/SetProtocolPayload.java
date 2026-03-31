package de.tyro.mcnetwork.network.payload;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.routing.protocol.AODVProtocol;
import de.tyro.mcnetwork.routing.protocol.DSRProtocol;
import de.tyro.mcnetwork.routing.protocol.IRoutingProtocol;
import de.tyro.mcnetwork.routing.protocol.LARProtocol;
import de.tyro.mcnetwork.routing.protocol.OLSRProtocol;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetProtocolPayload(String protocolName) implements CustomPacketPayload {

    public static final StreamCodec<ByteBuf, SetProtocolPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetProtocolPayload::protocolName,
            SetProtocolPayload::new);

    public static final CustomPacketPayload.Type<SetProtocolPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, "set_protocol"));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        if (context.flow().isServerbound()) {
            PacketDistributor.sendToAllPlayers(this);
        }
        context.enqueueWork(() -> {
            for (INetworkNode node : SimulationEngine.getInstance(context.flow().isClientbound()).getNodeList()) {
                node.setProtocol(createProtocol(protocolName, (ComputerBlockEntity) node));
            }
        }).exceptionally(ex -> {
            System.err.println("Error setting protocol: " + ex.getMessage());
            return null;
        });
    }

    private static IRoutingProtocol createProtocol(String protocolName, ComputerBlockEntity computer) {
        return switch (protocolName.toLowerCase()) {
            case "aodv" -> new AODVProtocol(computer);
            case "dsr" -> new DSRProtocol(computer);
            case "lar" -> new LARProtocol(computer);
            case "olsr" -> new OLSRProtocol(computer);
            default -> null;
        };
    }
}



