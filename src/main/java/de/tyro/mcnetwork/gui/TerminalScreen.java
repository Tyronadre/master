package de.tyro.mcnetwork.gui;

import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;
import de.tyro.mcnetwork.network.payload.TerminalUpdatePayload;
import de.tyro.mcnetwork.network.payload.TerminalWatchingPayload;
import de.tyro.mcnetwork.terminal.CommandRegistry;
import de.tyro.mcnetwork.terminal.Terminal;
import de.tyro.mcnetwork.terminal.commands.Command;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class TerminalScreen extends Screen {

    private final ComputerBlockEntity computerBlockEntity;
    private final Terminal terminal;
    private final Player player;

    /* -------- Input state -------- */

    private final StringBuilder inputBuffer = new StringBuilder();
    private int cursorPosition = 0;

    private int historyIndex = -1;

    /* -------- Completion --------- */

    private List<String> completionMatches = List.of();
    private int completionIndex = 0;
    private String completionBase = null;

    /* -------- Rendering -------- */

    private static final int PADDING = 8;
    private static final int LINE_HEIGHT = 10;
    private static final long CURSOR_BLINK_INTERVAL_MS = 500;

    public TerminalScreen(ComputerBlockEntity computer, Player player) {
        super(Component.literal("Terminal"));
        this.computerBlockEntity = computer;
        this.terminal = computer.getTerminal();
        this.terminal.setScreen(this);
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        this.setFocused(true);
        PacketDistributor.sendToServer(new TerminalWatchingPayload(computerBlockEntity.getBlockPos(), true));
    }

    @Override
    public void onClose() {
        super.onClose();
        PacketDistributor.sendToServer(new TerminalWatchingPayload(computerBlockEntity.getBlockPos(), false));
    }

    private void onChangeSend() {
        PacketDistributor.sendToServer(new TerminalUpdatePayload(computerBlockEntity.getBlockPos(), inputBuffer.toString(), cursorPosition));
    }

    public void onChangeReceive(TerminalUpdatePayload terminalUpdatePayload) {
        this.inputBuffer.setLength(0);
        this.inputBuffer.append(terminalUpdatePayload.input());
        this.cursorPosition = terminalUpdatePayload.cursorPosition();
    }

    /* ============================================================
       Input handling
       ============================================================ */

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        /* Ctrl+C */
        if (keyCode == GLFW.GLFW_KEY_C && hasControlDown()) {
            terminal.interrupt();
            return true;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER -> {
                submitCommand();
                onChangeSend();
                return true;
            }

            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursorPosition > 0) {
                    inputBuffer.deleteCharAt(cursorPosition - 1);
                    cursorPosition--;
                    onChangeSend();
                }
                return true;
            }

            case GLFW.GLFW_KEY_LEFT -> {
                if (cursorPosition > 0) cursorPosition--;
                onChangeSend();
                return true;
            }

            case GLFW.GLFW_KEY_RIGHT -> {
                if (cursorPosition < inputBuffer.length()) cursorPosition++;
                onChangeSend();
                return true;
            }

            case GLFW.GLFW_KEY_UP -> {
                historyUp();
                onChangeSend();
                return true;
            }

            case GLFW.GLFW_KEY_DOWN -> {
                historyDown();
                onChangeSend();
                return true;
            }

            case GLFW.GLFW_KEY_TAB -> {
                handleTabCompletion();
                onChangeSend();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void handleTabCompletion() {
        String currentInput = inputBuffer.substring(0, cursorPosition);
        int spaceCount = currentInput.length() - currentInput.replace(" ", "").length();

        if (completionBase == null) {
            if (spaceCount == 0) {
                // Completing command name
                completionBase = currentInput;
                completionMatches = CommandRegistry.INSTANCE.findMatching(currentInput);
            } else {
                // Completing argument
                String[] parts = currentInput.split(" ", -1);
                String commandName = parts[0];
                int argIndex = spaceCount - 1;
                String partial = parts[parts.length - 1];
                completionBase = partial;
                Command command = CommandRegistry.INSTANCE.get(commandName, terminal, new String[0]);
                if (command != null) {
                    completionMatches = command.getCompletions(argIndex, partial);
                } else {
                    completionMatches = List.of();
                }
            }
            completionIndex = 0;
        }

        if (completionMatches.isEmpty()) return;

        String match = completionMatches.get(completionIndex);
        completionIndex = (completionIndex + 1) % completionMatches.size();

        // Replace the current word
        String fullInput = inputBuffer.toString();
        int lastSpaceIndex = fullInput.lastIndexOf(' ', cursorPosition - 1);
        int startReplace = (lastSpaceIndex == -1) ? 0 : lastSpaceIndex + 1;
        int endReplace = cursorPosition;

        inputBuffer.replace(startReplace, endReplace, match);
        cursorPosition = startReplace + match.length();
        onChangeSend();
    }

    private void resetCompletion() {
        completionBase = null;
        completionMatches = List.of();
        completionIndex = 0;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!Character.isISOControl(codePoint)) {
            resetCompletion();
            inputBuffer.insert(cursorPosition, codePoint);
            cursorPosition++;
            onChangeSend();
            return true;
        }
        return false;
    }

    private void submitCommand() {
        String input = inputBuffer.toString().trim();
        if (!input.isEmpty()) {
            terminal.addToHistory(input);
            terminal.printLine(terminal.getPrompt() + input);
            terminal.submitInput(input);
        }
        inputBuffer.setLength(0);
        cursorPosition = 0;
        historyIndex = -1;
    }

    /* -------- History -------- */

    private void historyUp() {
        if (terminal.getCommandHistory().isEmpty()) return;

        if (historyIndex == -1) {
            historyIndex = terminal.getCommandHistory().size() - 1;
        } else if (historyIndex > 0) {
            historyIndex--;
        }

        loadHistoryEntry();
    }

    private void historyDown() {
        if (historyIndex == -1) return;

        historyIndex++;
        if (historyIndex >= terminal.getCommandHistory().size()) {
            historyIndex = -1;
            inputBuffer.setLength(0);
            cursorPosition = 0;
            return;
        }

        loadHistoryEntry();
    }

    private void loadHistoryEntry() {
        inputBuffer.setLength(0);
        inputBuffer.append(terminal.getCommandHistory().get(historyIndex));
        cursorPosition = inputBuffer.length();
    }

    /* ============================================================
       Rendering
       ============================================================ */


    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        Font font = Minecraft.getInstance().font;


        int availableWidth = width - 2 * PADDING;
        List<String> wrappedLines = new ArrayList<>();
        for (String line : terminal.getVisibleLines()) {
            String remaining = line;
            while (!remaining.isEmpty()) {
                String fit = font.plainSubstrByWidth(remaining, availableWidth);
                wrappedLines.add(fit);
                remaining = remaining.substring(fit.length());
            }
        }

        int maxLines = (height - PADDING * 2) / LINE_HEIGHT - 1;

        int start = Math.max(0, wrappedLines.size() - maxLines);
        int y = PADDING;

        /* Output */
        for (int i = start; i < wrappedLines.size(); i++) {
            guiGraphics.drawString(font, wrappedLines.get(i), PADDING, y, 0x00FF00);
            y += LINE_HEIGHT;
        }

        if (terminal.isBusy()) return;

        /* Prompt + Input */
        String prompt = terminal.getPrompt();
        guiGraphics.drawString(font, prompt, PADDING, y, 0x00FF00);

        int promptWidth = font.width(prompt);
        guiGraphics.drawString(font, inputBuffer.toString(), promptWidth + PADDING, y, 0x00FF00);

        /* Cursor */
        if (isCursorVisible()) {
            int cursorX = PADDING + promptWidth + font.width(inputBuffer.substring(0, cursorPosition));
            guiGraphics.fill(cursorX, y - 1, cursorX + 1, y + LINE_HEIGHT - 1, 0xFFFFFFFF);
        }

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isCursorVisible() {
        return (Util.getMillis() / CURSOR_BLINK_INTERVAL_MS) % 2 == 0;
    }

}
