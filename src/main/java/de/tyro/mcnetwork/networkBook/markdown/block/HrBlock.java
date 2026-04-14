package de.tyro.mcnetwork.networkBook.markdown.block;

import net.minecraft.client.gui.GuiGraphics;

public class HrBlock extends Block {
    @Override
    public int render(GuiGraphics gg, int x, int y, int width, int h) {
        if (gg != null) {
            int mid = y + lineHeight / 2;
            gg.fill(x, mid, x + width, mid + 1, 0xFF444444);
        }
        return lineHeight;
    }
}
