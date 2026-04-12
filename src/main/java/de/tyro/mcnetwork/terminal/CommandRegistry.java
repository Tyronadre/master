package de.tyro.mcnetwork.terminal;

import de.tyro.mcnetwork.terminal.commands.ClearCommand;
import de.tyro.mcnetwork.terminal.commands.Command;
import de.tyro.mcnetwork.terminal.commands.HelpCommand;
import de.tyro.mcnetwork.terminal.commands.IpConfigCommand;
import de.tyro.mcnetwork.terminal.commands.PingCommand;
import de.tyro.mcnetwork.terminal.commands.RoutingProtocolCommand;
import de.tyro.mcnetwork.terminal.commands.RoutingProtocolSettingsCommand;
import de.tyro.mcnetwork.terminal.commands.TraceRouteCommand;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CommandRegistry {
    public static final CommandRegistry INSTANCE = new CommandRegistry();

    private final HashMap<String, CommandFactory> commands = new HashMap<>();

    private CommandRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        register("ping", PingCommand::new);
        register("clear", ClearCommand::new);
        register("traceroute", TraceRouteCommand::new);
        register("tracert", TraceRouteCommand::new);
        register("help", HelpCommand::new);
        register("setProtocol", RoutingProtocolCommand::new);
        register("ipConfig", IpConfigCommand::new);
        register("routingProtocolSettings", RoutingProtocolSettingsCommand::new);
    }

    public void register(String name, CommandFactory command) {
        commands.put(name.toLowerCase(Locale.ROOT), command);
    }

    /**
     * @return a list of uninitialized commands that cannot be executed.
     */
    public List<Command> getCommands() {
        return commands.values().stream().map(it -> it.create(null, null)).toList();
    }

    /**
     * Creates a new command for the given terminal and the given arguments.
     * If there is no command found, returns null.
     *
     * @param name     the name of the command
     * @param terminal the terminal
     * @param args     the arguments
     * @return the command or null
     */
    public Command get(String name, Terminal terminal, String[] args) {
        if (!commands.containsKey(name.toLowerCase(Locale.ROOT))) return null;
        return commands.get(name.toLowerCase(Locale.ROOT)).create(terminal, args);
    }

    public boolean exists(String name) {
        return commands.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public List<String> findMatching(String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return commands.keySet().stream().filter(it -> it.startsWith(p)).sorted().collect(Collectors.toList());
    }


    /* ---------------- Factory ---------------- */

    @FunctionalInterface
    public interface CommandFactory {
        Command create(Terminal terminal, String[] args);
    }
}
