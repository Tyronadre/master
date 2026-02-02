package de.tyro.mcnetwork.terminal.commands;


import de.tyro.mcnetwork.terminal.Terminal;

public class TraceRouteCommand extends Command {

    public TraceRouteCommand(Terminal terminal, String[] args) {
        super(terminal, args);
    }

    @Override
    public String getName() {
        return "traceroute";
    }

    @Override
    public void execute() throws InterruptedException {
        if (args.length == 0) {
            println("usage: traceroute <host>");
            return;
        }

        String host = args[0];
        println("traceroute to " + host);

        for (int hop = 1; hop <= 8; hop++) {
            if (isCancelled()) return;

            sleep(800);
            println(hop + "  192.168.0." + hop + "  " + (10 + hop * 5) + " ms");
        }

        println("trace complete");
    }
}
