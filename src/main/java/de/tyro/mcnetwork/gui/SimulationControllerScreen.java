package de.tyro.mcnetwork.gui;

import de.tyro.mcnetwork.routing.core.SimulationNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class SimulationControllerScreen
        extends AbstractContainerScreen<SimulationControllerMenu> {

    public SimulationControllerScreen(
            SimulationControllerMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();

        int x = leftPos + 10;
        int y = topPos + 20;

        addRenderableWidget(
                Button.builder(Component.literal("Step"),
                                b -> SimulationNetwork.step())
                        .bounds(x, y, 80, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.literal("Run"),
                                b -> SimulationNetwork.run())
                        .bounds(x, y + 25, 80, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.literal("Pause"),
                                b -> SimulationNetwork.pause())
                        .bounds(x, y + 50, 80, 20)
                        .build()
        );

        addRenderableWidget(
                Button.builder(Component.literal("Start AODV"),
                                b -> SimulationNetwork.startAodv())
                        .bounds(x, y + 75, 80, 20)
                        .build()
        );
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gfx, float partialTick,
                            int mouseX, int mouseY) {
        // optional background
    }
}

