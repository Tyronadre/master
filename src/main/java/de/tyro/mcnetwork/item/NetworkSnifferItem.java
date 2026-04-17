package de.tyro.mcnetwork.item;

import de.tyro.mcnetwork.gui.NetworkSnifferScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class NetworkSnifferItem extends Item {

    public NetworkSnifferItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (level.isClientSide()) clientInit();
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @OnlyIn(Dist.CLIENT)
    private void clientInit() {
        Minecraft.getInstance().setScreen(new NetworkSnifferScreen());
    }
}
