package de.tyro.mcnetwork.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import static de.tyro.mcnetwork.MCNetwork.MODID;
import static de.tyro.mcnetwork.item.ItemRegistry.ITEMS;

public class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);


    public static final DeferredBlock<Block> COMPUTER = register("computer", (properties) -> new ComputerBlock(properties
            .strength(3.0F)
            .mapColor(MapColor.STONE)));



    private static <T extends Block> DeferredBlock<T> register(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> block = BLOCKS.registerBlock(name, function);
        registerBlockItem(name, block);
        return block;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ITEMS.registerItem(name, (properties) -> new BlockItem(block.get(), properties));
    }
}
