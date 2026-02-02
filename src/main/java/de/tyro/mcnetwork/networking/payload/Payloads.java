package de.tyro.mcnetwork.networking.payload;

import de.tyro.mcnetwork.networking.client.ClientPayloadHandler;
import de.tyro.mcnetwork.networking.server.ServerPayloadHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;

public class Payloads {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final var payloadRegistrar = event.registrar("1");

        payloadRegistrar.playBidirectional(
                SimulationControlPacket.TYPE,
                SimulationControlPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler::handleDataOnNetwork,
                        ServerPayloadHandler::handleDataOnNetwork
                )
        );
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.register(Payloads.class);
    }
}
