package de.tyro.mcnetwork.networkBook.markdown;// Packagenamen an dein Projekt anpassen

import com.mojang.blaze3d.systems.RenderSystem;
import de.tyro.mcnetwork.networkBook.markdown.block.Block;
import de.tyro.mcnetwork.networkBook.markdown.block.HeaderBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * MarkdownRenderer (Parser + Renderer)
 * <p.yaml>
 * - parse(text) -> ParsedDocument (blocks cached)
 * - render(gg, parsedDoc, startX, startY, width) -> draws content, returns total drawn height
 * - estimateHeight(parsedDoc, width) -> computes height without drawing (useful for scroll clamp)
 * <p.yaml>
 * Unterstützte Blocktypen: Heading, Paragraph, CodeBlock, ListBlock, TableBlock, Hr
 * Unterstützte Inline-Formatierungen: **bold**, *italic*, `inline code`
 * <p.yaml>
 * Wichtige Abhängigkeiten:
 * - Minecraft Font: Minecraft.getInstance().font
 * - GuiGraphics für drawString
 */
public class MarkdownRenderer {
    private final Minecraft mc = Minecraft.getInstance();

    // Basic line heights (adjustable)
    public static final int HEADING_SPACING = 6;
    public static final int PARAGRAPH_SPACING = 6;
    public static final int CODE_BLOCK_PADDING = 6;

    /**
     * Render parsed document at given position and width.
     * Returns total height drawn (useful to setup scroll limits).
     */
    public int render(GuiGraphics gg, MarkdownDocument doc, int startX, int startY, int width, int clipX, int clipY, int clipW, int clipH) {
        enableScissor(clipX, clipY, clipW, clipH);

        int y = startY;
        for (Block b : doc.blocks) {
            y += b.render(gg, startX, y, width, clipH);
            y += b instanceof HeaderBlock ? HEADING_SPACING : PARAGRAPH_SPACING;
        }

        disableScissor();
        return y - startY;
    }

    private void enableScissor(int x, int y, int w, int h) {
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        double scaleX = (double) mc.getWindow().getWidth() / screenWidth;
        double scaleY = (double) mc.getWindow().getHeight() / screenHeight;
        int sx = (int) Math.round(x * scaleX);
        int sy = (int) Math.round((screenHeight - (y + h)) * scaleY);
        int sw = (int) Math.round(w * scaleX);
        int sh = (int) Math.round(h * scaleY);
        RenderSystem.enableScissor(sx, sy, sw, sh);
    }

    private void disableScissor() {
        RenderSystem.disableScissor();
    }


}
