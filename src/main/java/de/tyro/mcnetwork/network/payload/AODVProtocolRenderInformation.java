package de.tyro.mcnetwork.network.payload;

import de.tyro.mcnetwork.MCNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AODVProtocolRenderInformation() implements CustomPacketPayload {
    public static final Type<AODVProtocolRenderInformation> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, AODVProtocolRenderInformation.class.getSimpleName().toLowerCase()));

    public static StreamCodec<FriendlyByteBuf, AODVProtocolRenderInformation> codec() {
        return new StreamCodec<FriendlyByteBuf, AODVProtocolRenderInformation>() {
            @Override
            public AODVProtocolRenderInformation decode(FriendlyByteBuf buffer) {
                return null;
            }

            @Override
            public void encode(FriendlyByteBuf buffer, AODVProtocolRenderInformation value) {

            }
        };
    }

    @Override

    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
