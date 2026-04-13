package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.routing.protocol.AODVProtocol;
import de.tyro.mcnetwork.routing.protocol.DSRProtocol;
import de.tyro.mcnetwork.routing.protocol.IRoutingProtocol;
import de.tyro.mcnetwork.routing.protocol.LARProtocol;
import de.tyro.mcnetwork.routing.protocol.OLSRProtocol;
import de.tyro.mcnetwork.terminal.Terminal;

import java.util.List;
import java.util.stream.Stream;

public class RoutingProtocolCommand extends Command{
    public RoutingProtocolCommand(Terminal terminal, String[] args) {
        super(terminal, args);
    }

    @Override
    public void execute() throws InterruptedException {
        if (this.args.length != 1) {
            println("Usage: setProtocol <protocol>[aodv|dsr|lar|olsr]");
            return;
        }

        IRoutingProtocol protocol = switch (this.args[0]) {
            case "aodv" -> new AODVProtocol(terminal.getNode());
            case "dsr" -> new DSRProtocol(terminal.getNode());
            case "lar" -> new LARProtocol(terminal.getNode());
            case "olsr" -> new OLSRProtocol(terminal.getNode());
            default -> {
                println("Usage: setProtocol <protocol>[aodv|dsr|lar|olsr] ");
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

    @Override
    public List<String> getCompletions(int argIndex, String partial) {
        if (argIndex == 0) {
            return Stream.of("aodv", "dsr", "lar", "olsr")
                    .filter(s -> s.startsWith(partial.toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
