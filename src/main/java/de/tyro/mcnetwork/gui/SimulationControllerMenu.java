package de.tyro.mcnetwork.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class SimulationControllerMenu extends AbstractContainerMenu {

    public static final MenuType<SimulationControllerMenu> TYPE = new MenuType<>(SimulationControllerMenu::new, FeatureFlags.DEFAULT_FLAGS);

    public SimulationControllerMenu(int id, Inventory playerInventory) {
        super(TYPE, id);
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

