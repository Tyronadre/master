package de.tyro.mcnetwork.gui.networkBook;

import de.tyro.mcnetwork.networkBook.data.SubTopic;
import de.tyro.mcnetwork.networkBook.markdown.MarkdownRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Vertical scrollable content area for a Subtopic.
 * - renders markdown content via MarkdownRenderer
 * - shows top-right close icon (X)
 * - shows bottom action button ("Thema verstanden")
 */
public class ContentPane implements GuiEventListener {

    private final int x, y, w, h;
    private float scrollY = 0f;
    private static SubTopic subtopic;
    private static MarkdownRenderer renderer;
    private Consumer<Void> onClose;
    private Consumer<SubTopic> onComplete;
    private int contentHeight;

    // UI elements positions (simple)
    private final int closeBtnXOffset = 16;
    private final int closeBtnYOffset = 8;
    private final int bottomButtonHeight = 20;
    private boolean focused;
    private boolean scrollbarDragging = false;
    private float scrollbarDragOffset = 0f;
    private GuiEventListener focusedElement = null;
    private List<GuiEventListener> interactiveBlocks = new ArrayList<>();

    public ContentPane(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public void setSubtopic(SubTopic s) {
        subtopic = s;
        renderer = s.getMarkdownRenderer();
        this.scrollY = 0f;
        interactiveBlocks.clear();
        focusedElement = null;
        interactiveBlocks = s.getMarkdownDocument().getInteractiveBlocks();
    }

    public void setCloseListener(Consumer<Void> c) {
        this.onClose = c;
    }

    public void setCompletionListener(Consumer<SubTopic> c) {
        this.onComplete = c;
    }

    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        // background
        gg.fill(x, y, x + w, y + h, 0xFF0E0E12);

        // topic content
        gg.pose().pushPose();
        gg.pose().translate(0, -scrollY, 0);
        contentHeight = renderer.render(gg, subtopic.getMarkdownDocument(), x + 8, y + 8, w - 28, x + 8, y + 8, w - 24, h - 40);
        gg.pose().popPose();

        // close topic
        int cx = x + w - 12;
        int cy = y + 8;
        gg.drawString(Minecraft.getInstance().font, "X", cx, cy, 0xAAFFFFFF);

        // topic done
        int by = y + h - bottomButtonHeight - 8;
        gg.fill(x + 8, by, x + w - 6, by + bottomButtonHeight, 0xFF224466);
        gg.drawCenteredString(Minecraft.getInstance().font, Component.literal("Thema verstanden"), x + w / 2, by + 4, 0xFFFFFFFF);

        // scrollbar
        if (contentHeight >= h - 32) {
            float percentage = (scrollY) / (contentHeight - h + 32);
            int sx = x + w - 12;
            int thumbHeight = 64;
            int trackTop = y + 20;
            int trackRange = h - 117;
            int sy = trackTop + (int) (percentage * Math.max(0, trackRange));
            int sy2 = sy + thumbHeight;
            gg.fill(sx, sy, sx + 6, sy2, 0xFFFFFFFF);
        }

    }

    public boolean mouseClicked(double mx, double my, int button) {
        // close icon
        int cx = x + w - closeBtnXOffset;
        int cy = y + closeBtnYOffset;
        if (mx >= cx && mx <= cx + 10 && my >= cy && my <= cy + 10) {
            if (onClose != null) onClose.accept(null);
            return true;
        }

        // bottom button
        int by = y + h - bottomButtonHeight - 8;
        if (mx >= x + 16 && mx <= x + w - 16 && my >= by && my <= by + bottomButtonHeight) {
            if (onComplete != null && subtopic != null) onComplete.accept(subtopic);
            if (onClose != null) onClose.accept(null);
            return true;
        }

        // scrollbar thumb click -> start dragging
        if (contentHeight >= h - 32) {
            int sx = x + w - 12;
            int thumbHeight = 64;
            int trackTop = y + 20;
            int trackRange = h - 117;
            if (trackRange > 0) {
                float range = (float) (contentHeight - h + 32);
                int sy = trackTop + (int) ((scrollY / Math.max(1f, range)) * trackRange);
                int sy2 = sy + thumbHeight;
                if (mx >= sx && mx <= sx + 6 && my >= sy && my <= sy2) {
                    scrollbarDragging = true;
                    scrollbarDragOffset = (float) (my - sy);
                    return true;
                }
            }
        }

        // delegate to interactive blocks
        for (GuiEventListener el : interactiveBlocks) {
            if (el.mouseClicked(mx, my + scrollY, button)) {
                focusedElement = el;
                return true;
            }
        }
        focusedElement = null;

        // else click in content area: start drag (we optionally allow dragging content)
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (scrollbarDragging) {
            int thumbHeight = 64;
            int trackTop = y + 20;
            int trackRange = h - 117;
            if (trackRange <= 0) return true;

            float thumbTop = (float) (my - scrollbarDragOffset);
            float minTop = trackTop;
            float maxTop = trackTop + trackRange;
            if (thumbTop < minTop) thumbTop = minTop;
            if (thumbTop > maxTop) thumbTop = maxTop;

            float percentage = (thumbTop - trackTop) / (float) trackRange;
            float contentRange = Math.max(0, contentHeight - h + 32);
            this.scrollY = percentage * contentRange;

            // clamp
            if (scrollY < 0) scrollY = 0;
            if (scrollY + h - 32 > contentHeight) scrollY = Math.max(0, contentHeight - h + 32);

            return true;
        }

        // not dragging scrollbar
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (scrollbarDragging) {
            scrollbarDragging = false;
            return true;
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

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (contentHeight < h - 32) return true;
        float scrollAmount = (float) (delta * 10.0);
        this.scrollY -= scrollAmount;
        if (scrollY < 0) {
            scrollY = 0;
            return true;
        }
        if (scrollY + h - 32 > contentHeight) scrollY = contentHeight - h + 32;
        return true;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (focusedElement != null) {
            return focusedElement.charTyped(codePoint, modifiers);
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedElement != null) {
            return focusedElement.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
}
