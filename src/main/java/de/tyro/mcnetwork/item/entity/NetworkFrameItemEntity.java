package de.tyro.mcnetwork.item.entity;

import de.tyro.mcnetwork.item.ItemRegistry;
import de.tyro.mcnetwork.routing.NetworkFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NetworkFrameItemEntity extends ItemEntity {

    private final NetworkFrame networkFrame;

    public NetworkFrameItemEntity(Level level, double x, double y, double z, NetworkFrame networkFrame) {
        super(ItemRegistry.PACKET_ITEM_ENTITY_TYPE.get(), level);
        this.setPos(x, y, z);
        this.setDeltaMovement(0, 0, 0);
        this.setItem(new ItemStack(ItemRegistry.PACKET_ITEM.get()));
        this.networkFrame = networkFrame;

        this.setNeverPickUp();
        this.noPhysics = true;
        this.lifespan = Integer.MAX_VALUE;
    }

    public NetworkFrameItemEntity(Level level) {
        this(level, 0, 0, 0, null);
    }

    public NetworkFrame getInFlightPacket() {
        return networkFrame;
    }

    @Override
    public void tick() {
        super.tick();

        // Move towards destination
        if (networkFrame.hasArrived()) {
            this.remove(RemovalReason.DISCARDED);
        } else {
            var pos = networkFrame.getCurrentPosition();
            this.setPos(pos.x(), pos.y(), pos.z());
        }
    }

}
