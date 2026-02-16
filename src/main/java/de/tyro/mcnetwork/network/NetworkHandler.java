package de.tyro.mcnetwork.network;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.network.configuration.SimulationConfigurationTask;
import de.tyro.mcnetwork.network.payload.ConfigAckPayload;
import de.tyro.mcnetwork.network.payload.ConfigSimulationEngineInitPayload;
import de.tyro.mcnetwork.network.payload.SimulationEngineSpeedPayload;
import de.tyro.mcnetwork.network.payload.TerminalUpdatePayload;
import de.tyro.mcnetwork.network.payload.TerminalWatchingPayload;
import de.tyro.mcnetwork.network.payload.routing.NewNetworkFramePayload;
import de.tyro.mcnetwork.network.payload.routing.NewNetworkPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = MCNetwork.MODID)
public class NetworkHandler {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MCNetwork.MODID).versioned("1");

        // ---------- CONFIG ---------- //

        registrar.configurationToServer(
                ConfigAckPayload.TYPE,
                ConfigAckPayload.STREAM_CODEC,
                ConfigAckPayload::handle
        );
        registrar.configurationToClient(
                ConfigSimulationEngineInitPayload.TYPE,
                ConfigSimulationEngineInitPayload.STREAM_CODEC,
                ConfigSimulationEngineInitPayload::handle
        );


        // ------ TERMINAL ----- //

        registrar.playToServer(
                TerminalWatchingPayload.TYPE,
                TerminalWatchingPayload.STREAM_CODEC,
                TerminalWatchingPayload::handleServerbound
        );

        registrar.playBidirectional(
                TerminalUpdatePayload.TYPE,
                TerminalUpdatePayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        TerminalUpdatePayload::handleClientbound,
                        TerminalUpdatePayload::handleServerbound
                )
        );

        registrar.playBidirectional(
                SimulationEngineSpeedPayload.TYPE,
                SimulationEngineSpeedPayload.STREAM_CODEC,
                SimulationEngineSpeedPayload::handle);

        registrar.playToServer(
                NewNetworkFramePayload.getType(),
                NewNetworkFramePayload.STREAM_CODEC,
                NewNetworkFramePayload::handle
        );

        registrar.playBidirectional(
                NewNetworkPacketPayload.getType(),
                NewNetworkPacketPayload.STEAM_CODEC,
                NewNetworkPacketPayload::handle
        );
    }

    @SubscribeEvent
    public static void register(RegisterConfigurationTasksEvent event) {
        event.register(new SimulationConfigurationTask(event.getListener()));
    }


}
