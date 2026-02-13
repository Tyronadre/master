package de.tyro.mcnetwork.item;

import de.tyro.mcnetwork.gui.networkBook.NetworkBookScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class NetworkBook extends Item {

    public NetworkBook(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (level.isClientSide()) clientInit();
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @OnlyIn(Dist.CLIENT)
    private void clientInit() {
        Minecraft.getInstance().setScreen(new NetworkBookScreen());
    }

}
