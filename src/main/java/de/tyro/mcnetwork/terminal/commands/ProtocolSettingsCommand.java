package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.simulation.protocol.IRoutingProtocol;
import de.tyro.mcnetwork.terminal.Terminal;

import java.util.ArrayList;
import java.util.List;

public class ProtocolSettingsCommand extends Command {
    public ProtocolSettingsCommand(Terminal terminal, String[] args) {
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
                var s = "";
                if (setting.isSettable()) s += "+ ";
                else s += "- ";
                println(setting.getKey() + ": " + setting.getValue());
            }
            return;
        }

        if (args.length > 3) {
            getHelp().forEach(this::println);
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

            //TODO
            // somehow sync this to the server. probably stop doing syncing in commands, but instead sync the whole terminal more?
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
                println(key + ": " + setting.getValue());
            }
            return;
        }

        getHelp().forEach(this::println);
    }

    @Override
    public List<String> getCompletions(int argIndex, String partial) {
        if (argIndex == 0) return List.of("set", "get");
        else if (argIndex == 1) return new ArrayList<>(terminal.getNode().getRoutingProtocol().getSettings().keys());
        return List.of();
    }

    @Override
    public String getName() {
        return "protocolSettings";
    }

    @Override
    protected List<String> getHelp() {
        return List.of(
                "Show and edit routing protocol settings",
                "Usage: protocolSettings [get <key>]|[set <key> <value>]",
                "protocolSettings shows all settings with their respective value. '+'marks settable variables",
                "protocolSettings get <key> shows the value for the given key, if exists",
                "protocolSettings set <key> value sets the value for the given key, if settable and exists");
    }
}
