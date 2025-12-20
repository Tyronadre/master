package de.tyro.mcnetwork.payloads.routing;

import net.minecraft.resources.ResourceLocation;

import static de.tyro.mcnetwork.MCNetwork.MODID;

public final class SimulationPayloads {

    public static final ResourceLocation STEP =
            ResourceLocation.fromNamespaceAndPath(MODID, "step");

    public static final ResourceLocation RUN =
            ResourceLocation.fromNamespaceAndPath(MODID, "run");

    public static final ResourceLocation START_AODV =
            ResourceLocation.fromNamespaceAndPath(MODID, "start_aodv");
}

