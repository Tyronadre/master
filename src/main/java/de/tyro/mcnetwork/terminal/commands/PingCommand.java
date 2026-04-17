package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.SimulationEngine;
import de.tyro.mcnetwork.simulation.exceptions.DestinationUnreachableException;
import de.tyro.mcnetwork.simulation.packet.application.PingPacket;
import de.tyro.mcnetwork.simulation.packet.application.PingRepPacket;
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
            println("usage: ping <host> [<timeout>]");
            return;
        }

        var destIPString = getOrThrow(String.class, 0);
        var timeout = getOrDefault(Integer.class, 1, 1000);

        if (!IP.validateIp(destIPString)) {
            println("invalid ip address: " + destIPString);
        }

        var destIP = new IP(destIPString);

        SimulationEngine sim = SimulationEngine.getInstance(terminal.getNode().getLevel().isClientSide());

        println("PING " + destIP);

        while (!isCancelled()) {
            long sendTime = sim.getSimTime();
            var ping = new PingPacket(terminal.getNode().getIP(), destIP, sendTime);

            terminal.getNode().sendApplicationPacket(ping, Integer.MAX_VALUE);

            // Warten auf Echo
            PingRepPacket rep;
            try {
                rep = terminal.getNode().getApplicationBus().waitFor(PingRepPacket.class, it -> it.replyUUID.equals(ping.id), timeout);
            } catch (DestinationUnreachableException due) {
                println("Destination unreachable");
                break;
            }

            long rtt = sim.getSimTime() - sendTime;
            if (rep != null) println("reply from " + destIP + ": time=" + rtt + " ms");
            else println("Request timed out");
        }
    }


}

