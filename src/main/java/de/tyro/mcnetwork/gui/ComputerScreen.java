package de.tyro.mcnetwork.gui;

import com.mojang.blaze3d.platform.InputConstants;
import de.tyro.mcnetwork.networking.NetworkUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ComputerScreen extends AbstractContainerScreen<ComputerMenu> {
    private EditBox terminalInput;
    private final List<String> terminalOutput = new ArrayList<>();

    public ComputerScreen(ComputerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos + 10;
        int y = this.topPos + this.imageHeight - 25;

        terminalInput = new EditBox(this.font, x, y, 160, 20, Component.literal("Command"));
        terminalInput.setMaxLength(50);
        terminalInput.setResponder(s -> {
        }); // no-op
        this.addRenderableWidget(terminalInput);

        terminalOutput.clear();
        terminalOutput.add("Welcome to NetCraft v0.1");
        terminalOutput.add("Type 'ipconfig' or 'help'");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);

        int x = this.leftPos + 10;
        int y = this.topPos + 10;
        for (String line : terminalOutput) {
            graphics.drawString(this.font, line, x, y, 0xFFFFFF, false);
            y += 10;
        }

        terminalInput.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            String cmd = terminalInput.getValue();
            handleCommand(cmd);
            terminalInput.setValue("");
            return true;
        }
        if (this.minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode)))
            return true;


        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void handleCommand(String cmd) {
        String[] parts = cmd.split(" ");
        switch (parts[0]) {
            case "help":
                terminalOutput.add("Available commands: ipconfig, setip <ip>, ping <ip>");
                break;

            case "ipconfig":
                terminalOutput.add("IP: " + this.menu.getBlockEntity().getIpAddress());
                terminalOutput.add("MAC: " + this.menu.getBlockEntity().getMacAddress());
                break;

            case "setip":
                if (parts.length == 2 && NetworkUtils.validateIp(parts[1])) {
                    this.menu.getBlockEntity().setIpAddress(parts[1]);
                    terminalOutput.add("IP set to " + parts[1]);
                } else {
                    terminalOutput.add("Usage: setip <x.x.x.x>");
                }
                break;

            case "ping":
                if (parts.length == 2) {
                    terminalOutput.add("Pinging " + parts[1] + " ...");
                    // TODO: Call networking system
                } else {
                    terminalOutput.add("Usage: ping <ip>");
                }
                break;

            default:
                terminalOutput.add("Unknown command: " + cmd);
        }
    }
}


