package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.routing.protocol.AODVProtocol;
import de.tyro.mcnetwork.routing.protocol.DSRProtocol;
import de.tyro.mcnetwork.routing.protocol.IRoutingProtocol;
import de.tyro.mcnetwork.terminal.Terminal;

public class RoutingProtocolCommand extends Command{
    public RoutingProtocolCommand(Terminal terminal, String[] args) {
        super(terminal, args);
    }

    @Override
    public void execute() throws InterruptedException {
        if (this.args.length != 1) {
            println("Usage: setProtocol <protocol>[aodv|dsr]");
            return;
        }

        IRoutingProtocol protocol = switch (this.args[0]) {
            case "aodv" -> new AODVProtocol(terminal.getNode());
            case "dsr" -> new DSRProtocol(terminal.getNode());
            default -> {
                println("Usage: setProtocol <protocol>[aodv|dsr] ");
                yield null;
            }
        };
        if (protocol == null) return;

        terminal.getNode().setProtocol(protocol);

    }

    @Override
    public String getName() {
        return "setProtocol";
    }
}
