package de.tyro.mcnetwork.item;

import de.tyro.mcnetwork.gui.networkBook.NetworkBookScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NetworkBook extends Item {

    public NetworkBook(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (level.isClientSide()) Minecraft.getInstance().setScreen(new NetworkBookScreen());
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

}
