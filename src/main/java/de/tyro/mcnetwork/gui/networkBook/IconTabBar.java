package de.tyro.mcnetwork.gui.networkBook;

import de.tyro.mcnetwork.networkBook.data.Topic;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Icon-only vertical tab bar (left).
 * - icons only
 * - tooltip on hover shows title
 */
public class IconTabBar implements GuiEventListener {

    private final int x, y, width;
    private final List<IconTab> tabs = new ArrayList<>();
    private final Consumer<Topic> onClick;
    private boolean focused;

    public IconTabBar(int x, int y, int width, Consumer<Topic> onClick) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.onClick = onClick;
    }

    public void setTopics(List<Topic> topics) {
        tabs.clear();
        int idxY = y;
        for (Topic t : topics) {
            ResourceLocation icon = t.getIcon(); // may be null -> default placeholder
            IconTab tab = new IconTab(x + 4, idxY, 32, 32, icon, t, onClick);
            tabs.add(tab);
            idxY += 44;
        }
    }

    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        // background panel
        gg.fill(x, y - 6, x + width, y + Math.max(200, tabs.size() * 44), 0xAA111111);

        for (IconTab t : tabs) t.render(gg, mouseX, mouseY, partialTicks);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        for (IconTab t : tabs) {
            if (t.mouseClicked(mx, my, button)) return true;
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }
}
