package de.tyro.mcnetwork.terminal;

import de.tyro.mcnetwork.gui.TerminalScreen;
import de.tyro.mcnetwork.simulation.INetworkNode;
import de.tyro.mcnetwork.terminal.commands.Command;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Terminal {

    private final List<String> outputBuffer = new ArrayList<>();
    private final Queue<String> inputQueue = new ConcurrentLinkedQueue<>();
    private final List<ServerPlayer> registeredPlayers = new ArrayList<>();
    private final INetworkNode node;

    private Thread runningCommandThread;
    private Command runningCommand;

    private final int maxLines = 200;
    private TerminalScreen screen;

    private final List<String> commandHistory = new ArrayList<>();

    public Terminal(INetworkNode node) {
        this.node = node;
    }

    /* ---------------- Input ---------------- */

    public void submitInput(String input) {
        if (isBusy()) {
            printLine("terminal busy – press Ctrl+C to abort");
            return;
        }
        inputQueue.add(input);
        processNextInput();
    }

    private void processNextInput() {
        String input = inputQueue.poll();
        if (input == null) return;

        String[] split = input.trim().split("\\s+");
        String cmdName = split[0].toLowerCase(Locale.ROOT);
        String[] args = Arrays.copyOfRange(split, 1, split.length);

        Command command = CommandRegistry.INSTANCE.get(cmdName, this, args);
        if (command == null) {
            printLine("command not found: " + cmdName);
            return;
        }
        runningCommand = command;

        runningCommandThread = new Thread(() -> {
            try {
                runningCommand.execute();
            } catch (InterruptedException ignored) {
                printLine("^C");
            } catch (Exception e) {
                printLine("Error Executing command: ");
                printLine(e.getMessage());
            } finally {
                runningCommand = null;
                runningCommandThread = null;
            }
        });

        runningCommandThread.start();
    }

    /* ---------------- Control ---------------- */

    public void interrupt() {
        if (runningCommand != null) {
            runningCommand.cancel();
            runningCommandThread.interrupt();
        }
    }

    public boolean isBusy() {
        return runningCommandThread != null;
    }

    /* ---------------- Output ---------------- */

    public void printLine(String line) {
        outputBuffer.add(line);
        if (outputBuffer.size() > maxLines) {
            outputBuffer.removeFirst();
        }
    }

    public void clear() {
        outputBuffer.clear();
    }

    /* ---------------- Rendering API ---------------- */

    public List<String> getVisibleLines() {
        return new ArrayList<>(outputBuffer);
    }

    public String getPrompt() {
        return "$ ";
    }

    public INetworkNode getNode() {
        return node;
    }


    public void notifyPlayers(CustomPacketPayload payload) {
        for (ServerPlayer watcher : registeredPlayers) {
            PacketDistributor.sendToPlayer(watcher, payload);
        }
    }

    public void registerPlayer(ServerPlayer player) {
            registeredPlayers.add(player);
    }

    public void unregisterPlayer(ServerPlayer player) {
        registeredPlayers.remove(player);
    }

    public void setScreen(TerminalScreen screen) {
        this.screen = screen;
    }

    public TerminalScreen getScreen() {
        return screen;
    }

    public void addToHistory(String command) {
        commandHistory.add(command);
    }

    public List<String> getCommandHistory() {
        return Collections.unmodifiableList(commandHistory);
    }

}
