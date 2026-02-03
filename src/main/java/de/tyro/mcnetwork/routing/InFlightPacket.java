package de.tyro.mcnetwork.routing;

import de.tyro.mcnetwork.routing.packet.NetworkPacket;

public class InFlightPacket {
    public final INetworkNode from;
    public final INetworkNode to;
    public final NetworkPacket packet;

    private double progress = 0; // 0.0 .. 1.0
    private static final double SPEED = 0.1; // pro tick

    public InFlightPacket(INetworkNode from, INetworkNode to, NetworkPacket packet) {
        this.from = from;
        this.to = to;
        this.packet = packet;
    }

    public boolean tick() {
        progress += SPEED;
        return progress >= 1.0;
    }

    public double getX() {
        return from.getX() + (to.getX() - from.getX()) * progress;
    }
    public double getY() {
        return from.getY() + (to.getY() - from.getY()) * progress;
    }
    public double getZ() {
        return from.getZ() + (to.getZ() - from.getZ()) * progress;
    }
}