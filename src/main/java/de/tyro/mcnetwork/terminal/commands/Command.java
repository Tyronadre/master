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

    /**
     * @return A list of Strings that will be printed when help is called on this command.
     */
    protected List<String> getHelp() {
        return List.of(getName());
    }

    /**
     * Get a variable from the arguments. If this argument is not of the correct type or does not exist, returns the defaultValue parameter
     * @param clazz the class that the variable should have.
     * @param argPos index in the argument array
     * @param defaultValue default return value
     * @return the value or the default value
     * @param <T> Class of the variable
     */
    protected <T> T getOrDefault(Class<T> clazz, int argPos, T defaultValue) {
        try {
            return getOrThrow(clazz, argPos);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    protected <T> T getOrThrow(Class<T> clazz, int argPos) {
        if  (argPos < 0 || argPos >= args.length) throw new IndexOutOfBoundsException("Argument out of bounds: " + argPos + ", " + args.length);

        var arg =  args[argPos];

        if (clazz.isAssignableFrom(arg.getClass())) return clazz.cast(arg);
        if (clazz == Integer.class) return (T) Integer.valueOf(arg);
        else if (clazz == Double.class) return (T) Double.valueOf(arg);
        else if (clazz == Short.class) return (T) Short.valueOf(arg);
        else if (clazz == String.class) return (T) arg;
        else throw new IllegalArgumentException("Unknown argument type: " + clazz);
    }
}
