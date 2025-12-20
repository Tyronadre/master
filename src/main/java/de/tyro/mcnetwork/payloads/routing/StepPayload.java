package de.tyro.mcnetwork.payloads.routing;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StepPayload() implements CustomPacketPayload {

    public static final Type<StepPayload> TYPE =
            new Type<>(SimulationPayloads.STEP);

    public static final Codec<StepPayload> CODEC = Codec.unit(new StepPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

