package de.tyro.mcnetwork.network.payload;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.gui.SimulationControllerScreen;
import de.tyro.mcnetwork.routing.SimulationEngine;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SimulationEngineSettingsPayload(double simSpeed, double frameSpeed, boolean paused, double commRadius) implements CustomPacketPayload {

    public static final StreamCodec<ByteBuf, SimulationEngineSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, SimulationEngineSettingsPayload::simSpeed,
            ByteBufCodecs.DOUBLE, SimulationEngineSettingsPayload::frameSpeed,
            ByteBufCodecs.BOOL, SimulationEngineSettingsPayload::paused,
            ByteBufCodecs.DOUBLE, SimulationEngineSettingsPayload::commRadius,
            SimulationEngineSettingsPayload::new);

    public static final CustomPacketPayload.Type<SimulationEngineSettingsPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, "simulation_engine_speed"));


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        var sim = SimulationEngine.getInstance(context.flow().isClientbound());
        sim.setSimSpeed(simSpeed());
        sim.setFrameMovementPerTick(frameSpeed());
        sim.setPaused(paused());
        sim.setCommRadius(commRadius());

        if (context.flow().isClientbound()) {
            if (Minecraft.getInstance().screen instanceof SimulationControllerScreen simScreen) {
                simScreen.simulationSpeedSlider.setValueExternal(simSpeed);
                simScreen.frameSpeedSlider.setValueExternal(frameSpeed);
                simScreen.commRadiusEditBox.setMessage(Component.literal(String.valueOf(commRadius())));
                simScreen.pauseButton.setMessage(Component.literal(paused ? "Resume" : "Pause"));
            }
        } else {
            PacketDistributor.sendToAllPlayers(this);
        }
    }

}
