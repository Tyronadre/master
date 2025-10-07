package de.tyro.mcnetwork.gui.networkBook;

import de.tyro.mcnetwork.networkBook.data.SubTopic;
import de.tyro.mcnetwork.networkBook.markdown.MarkdownRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.function.Consumer;

/**
 * Vertical scrollable content area for a Subtopic.
 * - renders markdown content via MarkdownRenderer
 * - shows top-right close icon (X)
 * - shows bottom action button ("Thema verstanden")
 */
public class ContentPane implements GuiEventListener {

    private final int x, y, w, h;
    private final MarkdownRenderer renderer = new MarkdownRenderer(Minecraft.getInstance());
    private float scrollY = 0f;
    private SubTopic subtopic;
    private Consumer<Void> onClose;
    private Consumer<SubTopic> onComplete;

    // UI elements positions (simple)
    private final int closeBtnXOffset = 16;
    private final int closeBtnYOffset = 8;
    private final int bottomButtonHeight = 20;
    private boolean focused;

    public ContentPane(int x, int y, int w, int h) {
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    public void setSubtopic(SubTopic s) {
        this.subtopic = s;
        this.scrollY = 0f; // reset scroll
    }

    public void setCloseListener(Consumer<Void> c) { this.onClose = c; }
    public void setCompletionListener(Consumer<SubTopic> c) { this.onComplete = c; }

    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
        // background
        gg.fill(x, y, x + w, y + h, 0xFF0E0E12);

        // top-right close icon
        int cx = x + w - closeBtnXOffset;
        int cy = y + closeBtnYOffset;
        gg.drawString(Minecraft.getInstance().font, "X", cx, cy, 0xFFFFFFFF);

        if (subtopic != null) {
            // content region (clip ideally)
            gg.pose().pushPose();
            gg.pose().translate(0, -scrollY, 0);

            // draw content via markdown renderer (start at x+12,y+24)
            var document = subtopic.getMarkdownDocument();
            int height = renderer.estimateHeight(document, w - 16);
            int drawn = renderer.render(gg, subtopic.getMarkdownDocument(), this.x + 8, (int) (this.y + 8 - scrollY), w - 16, x, y, w, h - 32);

            gg.pose().popPose();

            // bottom action button (simple rectangle)
            int by = y + h - bottomButtonHeight - 8;
            gg.fill(x + 16, by, x + w - 16, by + bottomButtonHeight, 0xFF224466);
            gg.drawCenteredString(Minecraft.getInstance().font, "Thema verstanden", x + w/2, by + 4, 0xFFFFFFFF);
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

        // else click in content area: start drag (we optionally allow dragging content)
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        // implement drag to scroll if wanted (also allow wheel)
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
        // scroll by wheel
        float scrollAmount = (float) (delta * 20.0);
        this.scrollY -= scrollAmount;
        clampScroll();
        return true;
    }

    private void clampScroll() {
        // clamp scroll based on content height from renderer
        if (subtopic == null) return;
        int contentHeight = renderer.estimateHeight(subtopic.getMarkdownDocument(), w - 32);
        if (contentHeight <= h - 60) {
            this.scrollY = 0;
            return;
        }
        if (scrollY < 0) scrollY = 0;
        float max = contentHeight - (h - 60);
        if (scrollY > max) scrollY = max;
    }
}
