package de.tyro.mcnetwork.routing.event;

import de.tyro.mcnetwork.routing.node.SimNode;
import de.tyro.mcnetwork.routing.packet.Packet;

public class PacketReceiveEvent implements SimulationEvent {

    private final SimNode sender;
    private final SimNode receiver;
    private final Packet packet;

    public PacketReceiveEvent(SimNode sender, SimNode receiver, Packet packet) {
        this.sender = sender;
        this.receiver = receiver;
        this.packet = packet;
    }

    public SimNode getSender() {
        return sender;
    }

    public SimNode getReceiver() {
        return receiver;
    }

    public Packet getPacket() {
        return packet;
    }

    @Override
    public void execute() {
        receiver.receive(packet);
    }

    @Override
    public String describe() {
        return "Node " + receiver.getId() + " received " + packet.getType();
    }
}
