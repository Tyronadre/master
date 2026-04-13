package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.terminal.Terminal;

import java.util.List;


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

    /** Returns completion suggestions for the given argument index and partial input */
    public List<String> getCompletions(int argIndex, String partial) {
        return List.of();
    }

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

    protected <T> T getOrDefault(Class<T> clazz, int argPos, T defaultValue) {
        if (argPos < 0 || argPos >= args.length) return defaultValue;

        var  arg = args[argPos];
        if (!clazz.isInstance(arg)) return defaultValue;
        return clazz.cast(arg);
    }

    protected <T> T getOrThrow(Class<T> clazz, int argPos) {
        if  (argPos < 0 || argPos >= args.length) throw new IndexOutOfBoundsException("Argument out of bounds: " + argPos + ", " + args.length);

        var arg =  args[argPos];
        if (!clazz.isInstance(arg)) throw new ClassCastException("Argument at pos " + argPos + " is not an instance of " + clazz.getName());
        return clazz.cast(arg);
    }
}
