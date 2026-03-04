package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.terminal.Terminal;

public class IpConfigCommand extends Command{
    public IpConfigCommand(Terminal terminal, String[] args) {
        super(terminal, args);
    }

    @Override
    public void execute() throws InterruptedException {
        println("IP Configuration");
        println("Pos: " + terminal.getNode().getBlockPos());
        println("Node: " + terminal.getNode().getIP());
        println("Protocol: " + terminal.getNode().getRoutingProtocol());
    }

    @Override
    public String getName() {
        return "ipConfig";
    }
}
