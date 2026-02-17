package de.tyro.mcnetwork;

import de.tyro.mcnetwork.block.BlockRegistry;
import de.tyro.mcnetwork.block.entity.BlockEntityRegistry;
import de.tyro.mcnetwork.entity.EntityRegistry;
import de.tyro.mcnetwork.gui.MenuRegistry;
import de.tyro.mcnetwork.item.ItemRegistry;
import de.tyro.mcnetwork.network.payload.networkPacket.NetworkPacketCodecGenerator;
import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.tabs.TabRegistry;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MCNetwork.MODID)
public class MCNetwork {
    public static final String MODID = "mcnetwork";
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public MCNetwork(IEventBus modEventBus, ModContainer modContainer) {
        BlockRegistry.register(modEventBus);
        BlockEntityRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        EntityRegistry.register(modEventBus);
        TabRegistry.register(modEventBus);
        MenuRegistry.register(modEventBus);


        NeoForge.EVENT_BUS.register(SimulationEngine.getInstance(false));
        NeoForge.EVENT_BUS.register(SimulationEngine.getInstance(true));

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NetworkPacketCodecGenerator.generate();
    }

}
