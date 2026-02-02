package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.terminal.Terminal;

public class PingCommand extends Command {

    public PingCommand(Terminal terminal, String[] args) {
        super(terminal, args);
    }

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public void execute() throws InterruptedException {
        if (args.length == 0) {
            println("usage: ping <host>");
            return;
        }

        String host = args[0];
        println("PING " + host + " with 32 bytes of data:");

        for (int i = 1; i <= 4; i++) {
            if (isCancelled()) return;

            sleep(1000);
            println("reply from " + host + ": bytes=32 time=" + (10 + i * 2) + "ms ttl=64");
        }

        println("ping statistics for " + host);
    }
}

