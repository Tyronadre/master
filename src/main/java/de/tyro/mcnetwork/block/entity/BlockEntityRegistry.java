package de.tyro.mcnetwork.block.entity;

import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.Tag;
import de.tyro.mcnetwork.block.BlockRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static de.tyro.mcnetwork.MCNetwork.MODID;

public class BlockEntityRegistry {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);

    public static final Supplier<BlockEntityType<ComputerBlockEntity>> COMPUTER_BE =
            BLOCK_ENTITIES.register("computer_be", () -> BlockEntityType.Builder.of(ComputerBlockEntity::new, BlockRegistry.COMPUTER.get()).build(null));


    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
