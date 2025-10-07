package de.tyro.mcnetwork.networkBook.markdown;

import net.minecraft.client.gui.GuiGraphics;

public class RenderInformation {
    private final GuiGraphics graphics;
    private final int x, y;
    private final int maxWidth;


    public RenderInformation(GuiGraphics graphics, int x, int y, int maxWidth) {
        this.graphics = graphics;
        this.x = x;
        this.y = y;
        this.maxWidth = maxWidth;
    }
}
