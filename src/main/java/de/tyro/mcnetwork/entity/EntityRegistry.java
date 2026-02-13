package de.tyro.mcnetwork.entity;

import de.tyro.mcnetwork.MCNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MCNetwork.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<NetworkFrameEntity>> NETWORK_FRAME_ENTITY = ENTITY_TYPES.register(
            "network_frame_entity",
            () -> EntityType.Builder.<NetworkFrameEntity>of((entityType, level) -> new NetworkFrameEntity(level), MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .eyeHeight(0.2125F)
                    .build("network_frame_entity"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
