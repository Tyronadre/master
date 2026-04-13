package de.tyro.mcnetwork.network.payload;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import de.tyro.mcnetwork.network.NetworkUtil;
import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.routing.protocol.AODVProtocol;
import de.tyro.mcnetwork.routing.protocol.DSRProtocol;
import de.tyro.mcnetwork.routing.protocol.LARProtocol;
import de.tyro.mcnetwork.routing.protocol.OLSRProtocol;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public record SetProtocolPayload(String routingProtocol, @Nullable BlockPos pos) implements CustomPacketPayload {

    public static Type<SetProtocolPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, SetProtocolPayload.class.getSimpleName().toLowerCase()));
    public static StreamCodec<FriendlyByteBuf, SetProtocolPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull SetProtocolPayload decode(@NotNull FriendlyByteBuf buffer) {
            return new SetProtocolPayload(Utf8String.read(buffer, 100), buffer.readNullable(buffer1 -> buffer1.readBlockPos()));
        }

        @Override
        public void encode(FriendlyByteBuf buffer, SetProtocolPayload value) {
            Utf8String.write(buffer, value.routingProtocol, 100);
            buffer.writeNullable(value.pos, (buffer2, value1) -> buffer2.writeBlockPos(value1));
        }
    };

    public void handle(IPayloadContext context) {
        var level = context.player().level();

        if (context.flow().isServerbound()) PacketDistributor.sendToAllPlayers(this);

        Consumer<INetworkNode> setter = node -> node.setProtocol(switch (routingProtocol) {
            case "AODVProtocol" -> new AODVProtocol(node);
            case "DSRProtocol" -> new DSRProtocol(node);
            case "LARProtocol" -> new LARProtocol(node);
            case "OLSRProtocol" -> new OLSRProtocol(node);
            default -> throw new IllegalArgumentException("Unknown routing protocol " + routingProtocol);
        });

        var sim = SimulationEngine.getInstance(context.flow().isClientbound());

        sim.setDefaultProtocol(setter);

        if (pos == null) {
           sim.getNodeList().forEach(setter);
        } else {
            setter.accept(NetworkUtil.getBlockEntityAt(ComputerBlockEntity.class, level, pos()));
        }

    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
