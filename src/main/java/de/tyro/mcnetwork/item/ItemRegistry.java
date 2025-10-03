package de.tyro.mcnetwork.item;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static de.tyro.mcnetwork.MCNetwork.MODID;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<NetworkBook> NETWORK_BOOK = ITEMS.registerItem("network_book", NetworkBook::new);
}
