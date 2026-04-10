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

public record SimulationEngineSettingsPayload(long simTime, double simSpeed, boolean paused, double commRadius, boolean receiveWindowActive) implements CustomPacketPayload {

    public static final StreamCodec<ByteBuf, SimulationEngineSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, SimulationEngineSettingsPayload::simTime,
            ByteBufCodecs.DOUBLE, SimulationEngineSettingsPayload::simSpeed,
            ByteBufCodecs.BOOL, SimulationEngineSettingsPayload::paused,
            ByteBufCodecs.DOUBLE, SimulationEngineSettingsPayload::commRadius,
            ByteBufCodecs.BOOL, SimulationEngineSettingsPayload::receiveWindowActive,
            SimulationEngineSettingsPayload::new);

    public static final CustomPacketPayload.Type<SimulationEngineSettingsPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, "simulation_engine_speed"));

    public static SimulationEngineSettingsPayloadBuilder Builder(SimulationEngine sim) {
        return new SimulationEngineSettingsPayloadBuilder(sim);
    }


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        var sim = SimulationEngine.getInstance(context.flow().isClientbound());
        sim.setSimSpeed(simSpeed());
        sim.setPaused(paused());
        sim.setCommRadius(commRadius());
        sim.setReceiveWindowActive(receiveWindowActive());

        if (context.flow().isClientbound()) {
            sim.setSimTime(simTime());
            if (Minecraft.getInstance().screen instanceof SimulationControllerScreen simScreen) {
                simScreen.simulationSpeedSlider.setValueExternal(simSpeed);
                simScreen.commRadiusEditBox.setSuggestion(String.valueOf(commRadius()));
                simScreen.pauseButton.setMessage(Component.literal(paused ? "Resume" : "Pause"));
                simScreen.receiveWindowToggleButton.setMessage(simScreen.getReceiveWindowLabel(sim));
            }
        } else {
            PacketDistributor.sendToAllPlayers(this);
        }
    }

    public static class SimulationEngineSettingsPayloadBuilder {
        long simTime;
        double simSpeed;
        double frameSpeed;
        boolean paused;
        double commRadius;
        private boolean receiveWindowActive;

        public SimulationEngineSettingsPayloadBuilder(SimulationEngine simulationEngine) {
            this.simTime = simulationEngine.getSimTime();
            this.simSpeed = simulationEngine.getSimSpeed();
            this.paused = simulationEngine.isPaused();
            this.commRadius = simulationEngine.getCommRange();
            this.receiveWindowActive = simulationEngine.receiveWindowActive();
        }

        public SimulationEngineSettingsPayloadBuilder simSpeed(double simSpeed) {
            this.simSpeed = simSpeed;
            return this;
        }

        public SimulationEngineSettingsPayloadBuilder frameSpeed(double frameSpeed) {
            this.frameSpeed = frameSpeed;
            return this;
        }

        public SimulationEngineSettingsPayloadBuilder paused(boolean paused) {
            this.paused = paused;
            return this;
        }

        public SimulationEngineSettingsPayloadBuilder commRadius(double commRadius) {
            this.commRadius = commRadius;
            return this;
        }

        public SimulationEngineSettingsPayload build() {
            return new SimulationEngineSettingsPayload(simTime, simSpeed, paused, commRadius, receiveWindowActive);
        }

        public SimulationEngineSettingsPayloadBuilder receiveWindowActive(boolean receiveWindowActive) {
            this.receiveWindowActive = receiveWindowActive;
            return this;
        }
    }



}
