package de.tyro.mcnetwork.item;

import de.tyro.mcnetwork.item.entity.NetworkFrameItemEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static de.tyro.mcnetwork.MCNetwork.MODID;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<NetworkBook> NETWORK_BOOK = ITEMS.registerItem("network_book", NetworkBook::new);
    public static final DeferredItem<SimulationContollerItem> SIM_CONTROLLER = ITEMS.registerItem("sim_controller", SimulationContollerItem::new);
    public static final DeferredItem<PacketItem> PACKET_ITEM = ITEMS.registerItem("packet_item", PacketItem::new);

    public static final DeferredRegister<EntityType<?>> ITEM_ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<NetworkFrameItemEntity>> PACKET_ITEM_ENTITY_TYPE = ITEM_ENTITIES.register(
            "packet_item_entity",
            () -> EntityType.Builder.<NetworkFrameItemEntity>of((entityType, level) -> new NetworkFrameItemEntity(level), MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .eyeHeight(0.2125F)
                    .clientTrackingRange(6)
                    .updateInterval(20)
                    .build("packet_item_entity"));


    public static void register(IEventBus eventBus) {
        ITEM_ENTITIES.register(eventBus);
        ITEMS.register(eventBus);
    }
}
