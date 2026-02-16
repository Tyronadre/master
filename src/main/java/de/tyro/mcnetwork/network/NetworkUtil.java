package de.tyro.mcnetwork.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NetworkUtil {
    public static <T extends BlockEntity> T getBlockEntityAt(Class<T> clazz, Level level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (clazz.isInstance(be)) {
            return clazz.cast(be);
        }
        return null;
    }
}

