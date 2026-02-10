package de.tyro.mcnetwork.network.payload;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.routing.SimulationEngine;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ConfigSimulationEngineInitPayload(double simulationTime, double simulationSpeed, double frameSpeed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConfigSimulationEngineInitPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, "simulation_engine_init"));
    public static final StreamCodec<ByteBuf, ConfigSimulationEngineInitPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, ConfigSimulationEngineInitPayload::simulationTime,
            ByteBufCodecs.DOUBLE, ConfigSimulationEngineInitPayload::simulationSpeed,
            ByteBufCodecs.DOUBLE, ConfigSimulationEngineInitPayload::frameSpeed,
            ConfigSimulationEngineInitPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        if (context.flow().isServerbound()) return;

        var sim = SimulationEngine.getInstance();
        sim.setSimSpeed(this.simulationSpeed);
        sim.setSimTime(this.simulationTime);
        sim.setFrameMovementPerTick(this.frameSpeed);

        context.reply(new ConfigAckPayload());

    }
}

