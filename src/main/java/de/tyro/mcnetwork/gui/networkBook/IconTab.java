package de.tyro.mcnetwork.gui.networkBook;

import de.tyro.mcnetwork.networkBook.data.Topic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Individual Icon Tab
 */
public class IconTab {

    private final int x, y, w, h;
    private final ResourceLocation icon;
    private final Topic topic;
    private final Consumer<Topic> onClick;

    public IconTab(int x, int y, int w, int h, ResourceLocation icon, Topic topic, Consumer<Topic> onClick) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.icon = icon;
        this.topic = topic;
        this.onClick = onClick;
    }

    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        // background
        int bg = isMouseOver(mouseX, mouseY) ? 0xFF2A2A2A : 0xFF1A1A1A;
        gg.fill(x, y, x + w, y + h, bg);

        if (icon != null) {
            if (icon.getPath().endsWith(".png"))
                gg.blit(icon, x + 2, y + 2, w - 4, h - 4, 0, 0, w - 4, h - 4, w - 4, h - 4);
            else {
                gg.pose().pushPose();
                gg.pose().scale(2,2,1);
                BuiltInRegistries.ITEM.getOptional(topic.getIcon()).ifPresent(it -> gg.renderFakeItem(new ItemStack(it.asItem()), x/2, y/2, 0));
                gg.pose().popPose();
            }

        } else {
            gg.fill(x + 6, y + 6, x + w - 6, y + h - 6, 0xFF33AA33);
        }

        if (isMouseOver(mouseX, mouseY)) {
            gg.renderTooltip(Minecraft.getInstance().font, Component.literal(topic.getTitle()), mouseX, mouseY);
        }
    }

    public boolean isMouseOver(double mx, double my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (isMouseOver(mx, my)) {
            if (onClick != null) onClick.accept(topic);
            return true;
        }
        return false;
    }
}
