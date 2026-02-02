package de.tyro.mcnetwork.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class SimulationControllerMenu extends AbstractContainerMenu {

    public SimulationControllerMenu(int id, Inventory playerInventory) {
        super(MenuRegistry.SIMULATION_MENU.get(), id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
