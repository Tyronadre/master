package de.tyro.mcnetwork;

import de.tyro.mcnetwork.block.entity.BlockEntityRegistry;
import de.tyro.mcnetwork.client.ComputerBlockEntityRenderer;
import de.tyro.mcnetwork.client.PacketItemEntityRenderer;
import de.tyro.mcnetwork.gui.MenuRegistry;
import de.tyro.mcnetwork.gui.SimulationControllerScreen;
import de.tyro.mcnetwork.item.ItemRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = MCNetwork.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = MCNetwork.MODID, value = Dist.CLIENT)
public class MCNetworkClient {
    public MCNetworkClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityRegistry.COMPUTER_BE.get(), ComputerBlockEntityRenderer::new);
        event.registerEntityRenderer(ItemRegistry.PACKET_ITEM_ENTITY_TYPE.get(), PacketItemEntityRenderer::new);
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistry.SIMULATION_MENU.get(), SimulationControllerScreen::new);
    }
}
