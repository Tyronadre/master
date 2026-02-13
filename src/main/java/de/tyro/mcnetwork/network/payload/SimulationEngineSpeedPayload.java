package de.tyro.mcnetwork.network.payload;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.gui.SimulationControllerScreen;
import de.tyro.mcnetwork.routing.SimulationEngine;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SimulationEngineSpeedPayload(double newSimulationSpeed, int playerID) implements CustomPacketPayload {

    public static final StreamCodec<ByteBuf, SimulationEngineSpeedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, SimulationEngineSpeedPayload::newSimulationSpeed,
            ByteBufCodecs.INT, SimulationEngineSpeedPayload::playerID,
            SimulationEngineSpeedPayload::new
    );

    public static final CustomPacketPayload.Type<SimulationEngineSpeedPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, "simulation_engine_speed"));


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        Player player = context.player();
        if (context.flow().isClientbound()) {
            if (player.getId() != this.playerID()) {
                SimulationEngine.getInstance().setSimSpeed(newSimulationSpeed());
                if (Minecraft.getInstance().screen instanceof SimulationControllerScreen simScreen) {
                    simScreen.simulationSpeedSlider.setValueExternal(newSimulationSpeed);
                }

            }
        } else {
            SimulationEngine.getInstance().setSimSpeed(newSimulationSpeed());
            PacketDistributor.sendToAllPlayers(this);
        }
    }

    public void handleConfig(IPayloadContext context) {
        SimulationEngine.getInstance().setSimSpeed(newSimulationSpeed());
    }


}
