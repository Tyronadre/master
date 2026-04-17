package de.tyro.mcnetwork.tabs;

import de.tyro.mcnetwork.block.BlockRegistry;
import de.tyro.mcnetwork.item.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static de.tyro.mcnetwork.MCNetwork.MODID;

public class TabRegistry {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final Supplier<CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register(MODID,
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(BlockRegistry.COMPUTER.get()))
                    .title(Component.translatable("creativetab.mcnetwork.creative_tab"))
                    .displayItems((itemDisplayParameters, output) -> {
                                output.accept(BlockRegistry.COMPUTER);
                                output.accept(ItemRegistry.NETWORK_BOOK);
                                output.accept(ItemRegistry.SIM_CONTROLLER);
                        output.accept(ItemRegistry.NETWORK_SNIFFER);
                            }
                    ).build()
    );

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
