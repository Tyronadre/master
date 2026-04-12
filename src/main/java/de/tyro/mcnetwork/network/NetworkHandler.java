package de.tyro.mcnetwork.network;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.network.configuration.SimulationConfigurationTask;
import de.tyro.mcnetwork.network.payload.ConfigAckPayload;
import de.tyro.mcnetwork.network.payload.NewNetworkFramePayload;
import de.tyro.mcnetwork.network.payload.NewNetworkPacketPayload;
import de.tyro.mcnetwork.network.payload.SetProtocolPayload;
import de.tyro.mcnetwork.network.payload.SimulationEngineSettingsPayload;
import de.tyro.mcnetwork.network.payload.TerminalUpdatePayload;
import de.tyro.mcnetwork.network.payload.TerminalWatchingPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
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
                SimulationEngineSettingsPayload.TYPE,
                SimulationEngineSettingsPayload.STREAM_CODEC,
                SimulationEngineSettingsPayload::handle
        );


        // ------ TERMINAL ----- //

        registrar.playBidirectional(
                SetProtocolPayload.TYPE,
                SetProtocolPayload.STREAM_CODEC,
                SetProtocolPayload::handle
        );

        registrar.playToServer(
                TerminalWatchingPayload.TYPE,
                TerminalWatchingPayload.STREAM_CODEC,
                TerminalWatchingPayload::handle
        );

        registrar.playBidirectional(
                TerminalUpdatePayload.TYPE,
                TerminalUpdatePayload.STREAM_CODEC,
                TerminalUpdatePayload::handle
        );

        registrar.playBidirectional(
                SimulationEngineSettingsPayload.TYPE,
                SimulationEngineSettingsPayload.STREAM_CODEC,
                SimulationEngineSettingsPayload::handle);

        registrar.playBidirectional(
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
