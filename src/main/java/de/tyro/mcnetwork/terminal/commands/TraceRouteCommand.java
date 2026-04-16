package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.SimulationEngine;
import de.tyro.mcnetwork.simulation.exceptions.DestinationUnreachableException;
import de.tyro.mcnetwork.simulation.packet.application.TraceRoutePacket;
import de.tyro.mcnetwork.simulation.packet.application.TraceRouteReplyPacket;
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

        var destIPString = args[0];

        if (!IP.validateIp(destIPString)) {
            println("invalid ip address: " + destIPString);
            return;
        }

        var destIP = new IP(destIPString);
        SimulationEngine sim = SimulationEngine.getInstance(terminal.getNode().getLevel().isClientSide());

        println("traceroute to " + destIP + " (max 30 hops)");

        boolean reachedDestination = false;

        for (int ttl = 1; ttl <= 30; ttl++) {
            if (isCancelled()) return;

            long sendTime = sim.getSimTime();
            var traceRoutePacket = new TraceRoutePacket(terminal.getNode().getIP(), destIP, sendTime);

            terminal.getNode().sendApplicationPacket(traceRoutePacket, ttl);

            // Warte auf Antwort (entweder vom Router (TTL überschritten) oder vom Ziel)
            TraceRouteReplyPacket rep;
            try {
                rep = terminal.getNode().getApplicationBus().waitFor(TraceRouteReplyPacket.class, 
                    it -> it.replyUUID.equals(traceRoutePacket.id), 5000);
            } catch (DestinationUnreachableException due) {
                println(ttl + " * * * (no response)");
                continue;
            }

            long rtt = sim.getSimTime() - sendTime;
            if (rep != null) {
                println(ttl + "  " + rep.getOriginatorIP() + "  " + rtt + " ms");
                
                // Wenn wir die Zieladresse erreicht haben, beenden
                if (rep.getOriginatorIP().equals(destIP)) {
                    reachedDestination = true;
                    break;
                }
            } else {
                println(ttl + " * * * (timeout)");
            }
        }

        if (reachedDestination) {
            println("trace complete - destination reached");
        } else {
            println("trace complete - destination not reachable");
        }
    }
}
