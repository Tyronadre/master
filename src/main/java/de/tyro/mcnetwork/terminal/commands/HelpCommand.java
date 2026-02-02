package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.terminal.CommandRegistry;
import de.tyro.mcnetwork.terminal.Terminal;

public class HelpCommand extends Command{
    public HelpCommand(Terminal terminal, String[] args) {
        super(terminal, args);
    }

    @Override
    public void execute() throws InterruptedException {
        println("available commands:");
        CommandRegistry.INSTANCE.getCommands().forEach(it -> println(it.getHelp()));

    }

    @Override
    public String getName() {
        return "help";
    }
}
