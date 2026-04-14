package de.tyro.mcnetwork.networkBook.markdown.block;

import de.tyro.mcnetwork.gui.networkBook.Scene3D.Scene3DRenderer;
import de.tyro.mcnetwork.gui.networkBook.Scene3D.SceneDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

//TODO
public class AnimationBlock extends Block {
    public final Scene3DRenderer renderer;

    public AnimationBlock(ResourceLocation location) {
        this.renderer = new Scene3DRenderer(SceneDefinition.load(location));
    }


    @Override
    public int render(GuiGraphics gg, int x, int y, int width, int h) {
        if (gg == null) return 0;
        try {
            gg.fill(x, y, 500, 500, 0xFF224466);
            gg.drawString(mc.font, "Test", x, y + 500, 0xFF0000);
            this.renderer.render(gg, x, y, width, 500, Minecraft.getInstance().getFrameTimeNs());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
