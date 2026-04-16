package de.tyro.mcnetwork.network.payload;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.gui.SimulationControllerScreen;
import de.tyro.mcnetwork.simulation.SimulationEngine;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SimulationEngineSettingsPayload(long simTime, double simSpeed, boolean paused, int commRadius, boolean receiveWindowActive, int receiveWindowMS, int receiveWindowSize) implements CustomPacketPayload {

    public static final StreamCodec<ByteBuf, SimulationEngineSettingsPayload> STREAM_CODEC = new StreamCodec<ByteBuf, SimulationEngineSettingsPayload>() {
        @Override
        public @NotNull SimulationEngineSettingsPayload decode(ByteBuf buffer) {
            return new SimulationEngineSettingsPayload(
                    buffer.readLong(),
                    buffer.readDouble(),
                    buffer.readBoolean(),
                    buffer.readInt(),
                    buffer.readBoolean(),
                    buffer.readInt(),
                    buffer.readInt()
            );
        }

        @Override
        public void encode(ByteBuf buffer, SimulationEngineSettingsPayload value) {
            buffer.writeLong(value.simTime());
            buffer.writeDouble(value.simSpeed());
            buffer.writeBoolean(value.paused());
            buffer.writeInt(value.commRadius());
            buffer.writeBoolean(value.receiveWindowActive());
            buffer.writeInt(value.receiveWindowMS());
            buffer.writeInt(value.receiveWindowSize());
        }
    };

    public static final CustomPacketPayload.Type<SimulationEngineSettingsPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, SimulationEngineSettingsPayload.class.getSimpleName().toLowerCase()));

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
        sim.setReceiveWindowMS(receiveWindowMS());
        sim.setReceiveWindowSize(receiveWindowSize());

        if (context.flow().isClientbound()) {
            sim.setSimTime(simTime());
            if (Minecraft.getInstance().screen instanceof SimulationControllerScreen simScreen) {
                simScreen.simulationSpeedSlider.setValueExternal(simSpeed);
                simScreen.simulationCommunicationRadius.setValue(commRadius);
                simScreen.simulationEnabledButton.setMessage(simScreen.simulationEnabledButtonLabel(sim));
                simScreen.receiveWindowEnabledButton.setMessage(simScreen.getReceiveWindowLabel(sim));
            }
        } else {
            PacketDistributor.sendToAllPlayers(this);
        }
    }

    public static class SimulationEngineSettingsPayloadBuilder {
        long simTime;
        double simSpeed;
        double frameSpeed;
        boolean simulationEnabled;
        int commRadius;
        boolean receiveWindowActive;
        int  receiveWindowMS;
        int receiveWindowSize;

        public SimulationEngineSettingsPayloadBuilder(SimulationEngine simulationEngine) {
            this.simTime = simulationEngine.getSimTime();
            this.simSpeed = simulationEngine.getSimSpeed();
            this.simulationEnabled = simulationEngine.isPaused();
            this.commRadius = simulationEngine.getCommRange();
            this.receiveWindowActive = simulationEngine.getReceiveWindowActive();
            this.receiveWindowMS = simulationEngine.getReceiveWindowMS();
            this.receiveWindowSize = simulationEngine.getReceiveWindowSize();
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
            this.simulationEnabled = paused;
            return this;
        }

        public SimulationEngineSettingsPayloadBuilder commRadius(int commRadius) {
            this.commRadius = commRadius;
            return this;
        }

        public SimulationEngineSettingsPayload build() {
            return new SimulationEngineSettingsPayload(simTime, simSpeed, simulationEnabled, commRadius, receiveWindowActive, receiveWindowMS, receiveWindowSize);
        }

        public SimulationEngineSettingsPayloadBuilder receiveWindowActive(boolean receiveWindowActive) {
            this.receiveWindowActive = receiveWindowActive;
            return this;
        }
    }



}
