package de.tyro.mcnetwork.datagen;

import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.item.ItemRegistry;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output,  ExistingFileHelper existingFileHelper) {
        super(output, MCNetwork.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ItemRegistry.NETWORK_BOOK.get());
        basicItem(ItemRegistry.SIM_CONTROLLER.get());
    }
}
