package de.tyro.mcnetwork.item;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static de.tyro.mcnetwork.MCNetwork.MODID;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<NetworkBook> NETWORK_BOOK = ITEMS.registerItem("network_book", NetworkBook::new);
    public static final DeferredItem<SimulationContollerItem> SIM_CONTROLLER = ITEMS.registerItem("sim_controller", SimulationContollerItem::new);
    public static final DeferredItem<NetworkSnifferItem> NETWORK_SNIFFER = ITEMS.registerItem("network_sniffer", NetworkSnifferItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
