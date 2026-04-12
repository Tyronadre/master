package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.routing.protocol.IRoutingProtocol;
import de.tyro.mcnetwork.terminal.Terminal;

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

        if (args.length == 0) {
            // Display all settings
            println("Routing Protocol Settings:");
            for (var entry : protocol.getSettings().entrySet()) {
                println(entry.getKey() + ": " + entry.getValue());
            }
            return;
        }

        if (args.length < 3 || !args[0].equals("set")) {
            println("Usage: routingProtocolSettings [set <key> <value>]");
            return;
        }

        String key = args[1];
        String valueStr = args[2];

        try {
            // Try to parse as int first
            try {
                int intValue = Integer.parseInt(valueStr);
                protocol.setSetting(key, intValue);
                println(key + " set to " + intValue);
            } catch (NumberFormatException e) {
                // Try as double
                double doubleValue = Double.parseDouble(valueStr);
                protocol.setSetting(key, doubleValue);
                println(key + " set to " + doubleValue);
            }
        } catch (Exception e) {
            println("Failed to set " + key + ": " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "routingProtocolSettings";
    }

    @Override
    protected String getHelp() {
        return "Show and edit routing protocol settings. Usage: routingProtocolSettings [set <key> <value>]";
    }
}
