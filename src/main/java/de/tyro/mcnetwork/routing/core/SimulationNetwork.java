package de.tyro.mcnetwork.routing.core;

import net.neoforged.neoforge.network.PacketDistributor;

public final class SimulationNetwork {

    public static void sendStep() {
        PacketDistributor.sendToServer(new StepPayload());
    }

    public static void sendRun() {
        PacketDistributor.sendToServer(new RunPayload(true));
    }

    public static void sendPause() {
        PacketDistributor.sendToServer(new RunPayload(false));
    }

    public static void sendStartAodv() {
        PacketDistributor.sendToServer(new StartAodvPayload());
    }
}

