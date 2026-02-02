package de.tyro.mcnetwork.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import de.tyro.mcnetwork.MCNetwork;
import de.tyro.mcnetwork.networking.payload.SimulationControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

public class SimulationControllerScreen extends AbstractContainerScreen<SimulationControllerMenu> {

    public SimulationControllerScreen(SimulationControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 6, 0x404040);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // optional: draw a background texture if you provide one
        // graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);

    }

    @Override
    protected void init() {
        super.init();
        int btnWidth = 80;
        int btnHeight = 20;
        int spacing = 5;
        int x = this.leftPos + (this.imageWidth - btnWidth) / 2;
        int y = this.topPos + 30;

        // Start Button
        this.addRenderableWidget(Button.builder(Component.literal("Start"), b -> sendControlPacket(SimulationControlPacket.Action.START))
                .bounds(x, y, btnWidth, btnHeight).build());
        // Stop Button
        this.addRenderableWidget(Button.builder(Component.literal("Stop"), b -> sendControlPacket(SimulationControlPacket.Action.STOP))
                .bounds(x, y + btnHeight + spacing, btnWidth, btnHeight).build());
        // Reset Button
        this.addRenderableWidget(Button.builder(Component.literal("Reset"), b -> sendControlPacket(SimulationControlPacket.Action.RESET))
                .bounds(x, y + 2 * (btnHeight + spacing), btnWidth, btnHeight).build());
        // Tick Button (optional)
        this.addRenderableWidget(Button.builder(Component.literal("Tick"), b -> sendControlPacket(SimulationControlPacket.Action.TICK))
                .bounds(x, y + 3 * (btnHeight + spacing), btnWidth, btnHeight).build());
    }

    private void sendControlPacket(SimulationControlPacket.Action action) {
        PacketDistributor.sendToServer(new SimulationControlPacket(action));
    }
}
