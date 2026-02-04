package de.tyro.mcnetwork.item.entity;

import de.tyro.mcnetwork.item.ItemRegistry;
import de.tyro.mcnetwork.routing.InFlightPacket;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PacketItemEntity extends ItemEntity {

    private final InFlightPacket packet;

    public PacketItemEntity(Level level, double x, double y, double z, InFlightPacket packet) {
        super(ItemRegistry.PACKET_ITEM_ENTITY_TYPE.get(), level);
        this.setPos(x, y, z);
        this.setDeltaMovement(0, 0, 0);
        this.setItem(new ItemStack(ItemRegistry.PACKET_ITEM.get()));
        this.packet = packet;

        this.setNeverPickUp();
        this.noPhysics = true;
        this.lifespan = Integer.MAX_VALUE;
    }

    public PacketItemEntity(Level level) {
        this(level, 0, 0, 0, null);
    }

    public InFlightPacket getInFlightPacket() {
        return packet;
    }

    public INetworkPacket getNetworkPacket() {
        return packet.packet;
    }

    @Override
    public void tick() {
        super.tick();

        // Move towards destination
        if (packet.tick()) {
            this.remove(RemovalReason.DISCARDED);
        } else {
            var pos = packet.getCurrentPosition();
            this.setPos(pos.x(), pos.y(), pos.z());
        }
    }
}
