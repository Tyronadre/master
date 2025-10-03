package de.tyro.mcnetwork.gui;

import de.tyro.mcnetwork.block.BlockRegistry;
import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class ComputerMenu extends AbstractContainerMenu {
    private final ComputerBlockEntity computer;
    private final Level level;

    public ComputerMenu(int id, Inventory playerInventory, BlockEntity computer) {
        super(MenuRegistry.COMPUTER_MENU.get(), id);
        this.computer = (ComputerBlockEntity) computer;
        this.level = playerInventory.player.level();
    }

    public ComputerMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ComputerBlockEntity getBlockEntity() {
        return computer;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, computer.getBlockPos()), player, BlockRegistry.COMPUTER.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return null;
    }
}

