package de.tyro.mcnetwork.networkBook.markdown;

import de.tyro.mcnetwork.networkBook.markdown.block.AnimationBlock;
import de.tyro.mcnetwork.networkBook.markdown.block.Block;
import de.tyro.mcnetwork.networkBook.markdown.block.CodeBlock;
import de.tyro.mcnetwork.networkBook.markdown.block.HeaderBlock;
import de.tyro.mcnetwork.networkBook.markdown.block.HrBlock;
import de.tyro.mcnetwork.networkBook.markdown.block.ImageBlock;
import de.tyro.mcnetwork.networkBook.markdown.block.ListBlock;
import de.tyro.mcnetwork.networkBook.markdown.block.ParagraphBlock;
import de.tyro.mcnetwork.networkBook.markdown.block.TableBlock;
import de.tyro.mcnetwork.networkBook.markdown.block.TaskBlock;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MarkdownDocument {
    final List<Block> blocks = new ArrayList<>();

    public void addImageBlock(ResourceLocation resourceLocation, String title) {
        blocks.add(new ImageBlock(resourceLocation, title));
    }

    public void addAnimationBlock(ResourceLocation resourceLocation) {
        blocks.add(new AnimationBlock(resourceLocation));
    }

    public void addCodeBlock(String string, String codeLang) {
        blocks.add(new CodeBlock(string, codeLang));
    }

    public void addHrBlock() {
        blocks.add(new HrBlock());
    }

    public void addHeaderBlock(int level, String text) {
        blocks.add(new HeaderBlock(level, text));
    }

    public void addParagraphBlock(List<Block.InlineNode> inlineNodes) {
        blocks.add(new ParagraphBlock(inlineNodes));
    }

    public void addTableBlock(List<List<String>> rows, boolean header) {
        blocks.add(new TableBlock(rows, header));
    }

    public void addListBlock(List<List<Block.InlineNode>> list, boolean ordered) {
        blocks.add(new ListBlock(list, ordered));
    }

    public void addTaskBlock(List<List<Block>> questions, List<String> answers) {
        blocks.add(new TaskBlock(questions, answers));
    }

    public List<GuiEventListener> getInteractiveBlocks() {
        return blocks.stream().filter(it -> it instanceof GuiEventListener).map(it -> (GuiEventListener) it).collect(Collectors.toList());
    }
}
