package de.tyro.mcnetwork.terminal.commands;

import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.routing.packet.PingPacket;
import de.tyro.mcnetwork.routing.packet.PingRepPacket;
import de.tyro.mcnetwork.routing.protocol.RoutingProtocol;
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

        var destIPString = args[0];

        if (!IP.validateIp(destIPString)) {
            println("invalid ip address: " + destIPString);
        }

        var destIP = new IP(destIPString);

        SimulationEngine sim = SimulationEngine.getInstance();
        RoutingProtocol proto = terminal.getNode().getRoutingProtocol();

        println("PING " + destIP);

        //If we dont have a route, ask our protocol to find one
        if (!proto.hasRoute(destIP)) proto.onSendRequest(terminal.getNode(), destIP);

        //block until we have a route
        while (!proto.hasRoute(destIP)) {
            sleep(50);
        }

        while (!isCancelled()) {
            long sendTime = sim.getSimTime();
            var ping = new PingPacket(terminal.getNode().getIP(), destIP, sendTime);
            System.out.println("Sending ping " + ping);
            proto.sendData(terminal.getNode(), ping);

            // Warten auf Echo
            PingRepPacket rep = terminal.getNode().getApplicationBus().waitFor(PingRepPacket.class, it -> it.replyUUID.equals(ping.id), 50000);
            System.out.println("Got reply " + rep);

            long rtt = sim.getSimTime() - sendTime;
            if (rep != null) println("reply from " + destIP + ": time=" + rtt + " ms");
            else println("Request timed out");
        }
    }
}

