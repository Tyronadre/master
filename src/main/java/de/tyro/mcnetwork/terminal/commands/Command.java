package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.terminal.Terminal;


public abstract class Command {

    protected final Terminal terminal;
    protected final String[] args;
    private boolean cancelled = false;

    public Command(Terminal terminal, String[] args) {
        this.terminal = terminal;
        this.args = args;
    }

    /** Wird beim Start genau einmal aufgerufen */
    public abstract void execute() throws InterruptedException;

    /** Optionaler Name */
    public abstract String getName();

    /** Ctrl+C */
    public void cancel() {
        cancelled = true;
    }

    protected boolean isCancelled() {
        return cancelled;
    }

    protected void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    protected void println(String text) {
        terminal.printLine(text);
    }

    protected String getHelp() {
        return getName();
    }
}

