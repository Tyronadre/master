package de.tyro.mcnetwork.terminal;

import net.minecraft.core.BlockPos;

public record TerminalCommandPackage(BlockPos pos, String command, String[] arguments) {

}
