package de.tyro.mcnetwork.item;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static de.tyro.mcnetwork.MCNetwork.MODID;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<NetworkBook> NETWORK_BOOK = ITEMS.registerItem("network_book", NetworkBook::new);
    public static final DeferredItem<SimulationContollerItem> SIM_CONTROLLER = ITEMS.registerItem("sim_controller", SimulationContollerItem::new);
    public static final DeferredItem<PacketItem> PACKET_ITEM = ITEMS.registerItem("packet_item", PacketItem::new);
}
