package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.routing.protocol.IRoutingProtocol;
import de.tyro.mcnetwork.routing.protocol.ProtocolSettings;
import de.tyro.mcnetwork.terminal.Terminal;

import java.util.ArrayList;
import java.util.List;

public class RoutingProtocolSettingsCommand extends Command {
    public RoutingProtocolSettingsCommand(Terminal terminal, String[] args) {
        super(terminal, args);
    }

    @Override
    public void execute() throws InterruptedException {
        IRoutingProtocol protocol = terminal.getNode().getRoutingProtocol();
        if (protocol == null) {
            println("No routing protocol set.");
            return;
        }

        var settings = protocol.getSettings();

        if (args.length == 0) {
            println("Settings for " + protocol.getClass().getSimpleName());

            if (protocol.getSettings().size() == 0) {
                println("No routing protocol settings found.");
                return;
            }

            println("Routing Protocol Settings:");
            for (var setting : protocol.getSettings().getAll()) {
                println(setting.getKey() + ": " + setting.getValue());
            }
            return;
        }

        if (args.length > 3) {
            println("Usage: routingProtocolSettings [get <key>]|[set <key> <value>]");
            return;
        }

        if (args[0].equals("set")) {
            String key = args[1];
            var setting = settings.getSetting(key);

            if (setting == null) {
                println("Unknown setting: " + key);
                return;
            }

            if (!setting.isSettable()) {
                println("Setting " + key + " is not settable.");
                return;
            }

            setting.setValue(getOrThrow(setting.getValueClass(), 2));

            println("Setting " + key + " set to " + setting.getValue() + ".");
            return;
        }

        if (args[0].equals("get")) {
            String key = args[1];
            var setting = settings.getSetting(key);
            if (setting == null) {
                println("Unknown setting: " + key);
            } else {
                println(key + ": " +   setting.getValue());
            }
            return;
        }

        println("Usage: routingProtocolSettings [get <key>]|[set <key> <value>]");
    }

    @Override
    public List<String> getCompletions(int argIndex, String partial) {
        if (argIndex == 0) return List.of("set", "get");
        else if (argIndex == 1) return new ArrayList<>(terminal.getNode().getRoutingProtocol().getSettings().keys());
        return List.of();
    }

    @Override
    public String getName() {
        return "routingProtocolSettings";
    }

    @Override
    protected String getHelp() {
        return "Show and edit routing protocol settings. \n Usage: routingProtocolSettings [get <key>]|[set <key> <value>]";

    }
}
