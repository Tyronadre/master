package de.tyro.mcnetwork.network.payload;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import de.tyro.mcnetwork.network.BetterByteBuf;
import de.tyro.mcnetwork.network.NetworkUtil;
import de.tyro.mcnetwork.simulation.INetworkNode;
import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.SimulationEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record RegisterNetworkNodePayload(IP ip, BlockPos pos) implements CustomPacketPayload {
    public static final Type<RegisterNetworkNodePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, RegisterNetworkNodePayload.class.getSimpleName().toLowerCase()));
    public static final StreamCodec<FriendlyByteBuf, RegisterNetworkNodePayload> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, RegisterNetworkNodePayload>() {
        @Override
        public RegisterNetworkNodePayload decode(FriendlyByteBuf buffer) {
            var buf = new BetterByteBuf(buffer);
            return new RegisterNetworkNodePayload(
                    buf.readIP(),
                    buf.readBlockPos()
            );
        }

        @Override
        public void encode(FriendlyByteBuf buffer, RegisterNetworkNodePayload value) {
            var buf = new BetterByteBuf(buffer);
            buf.writeIP(value.ip);
            buf.writeBlockPos(value.pos);
        }
    };

    public RegisterNetworkNodePayload(INetworkNode node) {
        this(node.getIP(), node.getBlockPos());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        var level = context.player().level();
        var node = NetworkUtil.getBlockEntityAt(ComputerBlockEntity.class, level, pos());

        if (node == null) return;

        node.setIP(ip);
        SimulationEngine.getInstance(true).registerNode(node);
    }
}
