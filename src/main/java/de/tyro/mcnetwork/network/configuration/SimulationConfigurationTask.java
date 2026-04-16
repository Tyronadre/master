package de.tyro.mcnetwork.network.configuration;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.network.payload.SimulationEngineSettingsPayload;
import de.tyro.mcnetwork.simulation.SimulationEngine;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public record SimulationConfigurationTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
    public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, "simulation_configuration_task"));

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        var sim = SimulationEngine.getInstance(false);
        var payload = SimulationEngineSettingsPayload.Builder(sim).build();
        sender.accept(payload);

        listener().finishCurrentTask(TYPE);
    }

    @Override
    public @NotNull Type type() {
        return TYPE;
    }
}
