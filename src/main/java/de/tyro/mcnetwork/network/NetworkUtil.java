package de.tyro.mcnetwork.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

public class NetworkUtil {
    public static <T extends BlockEntity> T getBlockEntityAt(Class<T> clazz, Level level, Vec3 vec) {
        return getBlockEntityAt(clazz, level, new BlockPos((int) vec.x, (int) vec.y, (int) vec.z));
    }

    public static <T extends BlockEntity> T getBlockEntityAt(Class<T> clazz, Level level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (clazz.isInstance(be)) {
            return clazz.cast(be);
        }
        return null;
    }

    public static <T extends BlockEntity> T getBlockEntityAt(Class<T> clazz, GlobalPos pos) {
//        return getBlockEntityAt(clazz, Levelpos.dimension().)
        return null;
    }
}
