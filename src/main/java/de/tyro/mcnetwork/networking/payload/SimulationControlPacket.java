package de.tyro.mcnetwork.networking.payload;

import de.tyro.mcnetwork.MCNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SimulationControlPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SimulationControlPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MCNetwork.MODID, SimulationControlPacket.class.getSimpleName().toLowerCase()));

    public static final StreamCodec<ByteBuf, SimulationControlPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, SimulationControlPacket::action, SimulationControlPacket::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    private static Integer action(SimulationControlPacket simulationControlPacket) {
        return simulationControlPacket.action.ordinal();
    }

    public final Action action;

    public SimulationControlPacket(Integer action) {
        this.action = Action.fromOrdinal(action);
    }

    public SimulationControlPacket(Action action) {
        this.action = action;
    }


    public enum Action {
        START, STOP, RESET, TICK;

        public static Action fromOrdinal(int ord) {
            return values()[ord];
        }
    }

}
