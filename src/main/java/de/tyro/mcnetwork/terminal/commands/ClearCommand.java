package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.terminal.Terminal;

public class ClearCommand extends Command {

    public ClearCommand(Terminal terminal, String[] args) {
        super(terminal, args);
    }

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public void execute() {
        terminal.clear();
    }
}
