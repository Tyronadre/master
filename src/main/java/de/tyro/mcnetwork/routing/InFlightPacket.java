package de.tyro.mcnetwork.routing;

import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import net.minecraft.world.phys.Vec3;

public class InFlightPacket {
    public final INetworkNode from;
    public final INetworkNode to;
    public final INetworkPacket packet;

    private double traveled = 0;
    private static final double SPEED = 0.05; // pro tick

    public InFlightPacket(INetworkNode from, INetworkNode to, INetworkPacket packet) {
        this.from = from;
        this.to = to;
        this.packet = packet;
    }

    public boolean tick() {
        traveled += SPEED;
        return traveled >= from.getPos().distanceTo(to.getPos());
    }

    public Vec3 getCurrentPosition() {
        double total = from.getPos().distanceTo(to.getPos());
        double t = Math.min(1.0, traveled / total);
        return from.getPos().lerp(to.getPos(), t);
    }
}