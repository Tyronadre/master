package de.tyro.mcnetwork.networkBook.markdown.block;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;

public class ImageBlock extends Block {
    public final ResourceLocation location;
    public final String title;
    public final int height;
    public final int width;

    public ImageBlock(ResourceLocation location, String title) {
        this.location = location;
        this.title = title;

        int tHeight = 0;
        int tWidth = 0;

        Minecraft mc = Minecraft.getInstance();
        try {
            Resource res = mc.getResourceManager().getResource(location).orElse(null);
            if (res != null) {
                InputStream in = res.open();
                NativeImage imgData = NativeImage.read(in);
                tWidth = imgData.getWidth() / 2;
                tHeight = imgData.getHeight() / 2;
                imgData.close();
                in.close();
            }
        } catch (Exception e) {
            System.err.println("Failed to load image " + location);
        }

        height = tHeight;
        width = tWidth;
    }

    @Override
    public int render(GuiGraphics gg, int x, int y, int width, int h) {
        if (location == null) return 0;


        int drawW = this.width;
        int drawH = this.height;
        float aspect = (float) drawW / (float) drawH;


        if (drawW > width) {
            drawW = width;
            drawH = (int) (width / aspect);
        }

        int drawX = x + (width - drawW) / 2;

        if (gg != null) {
            gg.blit(location, drawX, y, 0, 0, drawW, drawH, this.width, this.height);
        }

        int totalHeight = drawH;

        if (title != null && !title.isEmpty()) {
            int textWidth = mc.font.width(title);
            int textX = x + (width - textWidth) / 2;
            int textY = y + drawH + 4;
            if (gg != null) {
                gg.drawString(mc.font, title, textX, textY, 0xFFFFFF);
            }
            totalHeight += mc.font.lineHeight + 6;
        }

        return totalHeight + 8;
    }
}
