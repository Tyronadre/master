package de.tyro.mcnetwork.item;

import de.tyro.mcnetwork.gui.SimulationControllerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class SimulationContollerItem extends Item {
    public SimulationContollerItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        if (!level.isClientSide)  openMenu(player);
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    private void openMenu(Player player) {
        player.openMenu(new SimpleMenuProvider(
                (id, inv, ply) -> new SimulationControllerMenu(id),
                Component.literal("Simulation Controller")
        ));
    }

}
