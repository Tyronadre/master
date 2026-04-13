package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.terminal.CommandRegistry;
import de.tyro.mcnetwork.terminal.Terminal;

public class HelpCommand extends Command {
    public HelpCommand(Terminal terminal, String[] args) {
        super(terminal, args);
    }

    @Override
    public void execute() throws InterruptedException {
        if (args.length == 0)
            CommandRegistry.INSTANCE.getCommands().forEach(it -> println(it.getName()));
        if (args.length == 1) {
            println(CommandRegistry.INSTANCE.get(getOrThrow(String.class, 0), terminal, null).getHelp());
        }
    }

    @Override
    public String getName() {
        return "help";
    }
}
