package de.tyro.mcnetwork.network.payload;

import de.tyro.mcnetwork.terminal.Terminal;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class TerminalUpdatePayload implements CustomPacketPayload {


    public static TerminalUpdatePayload of(Terminal terminal) {
        return null;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return null;
    }
}
