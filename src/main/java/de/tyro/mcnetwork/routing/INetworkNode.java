package de.tyro.mcnetwork.routing;

import de.tyro.mcnetwork.routing.packet.NetworkPacket;
import de.tyro.mcnetwork.routing.protocol.RoutingProtocol;
import net.minecraft.world.phys.Vec3;

public interface INetworkNode {

    IP getIP();

    double getX();
    double getY();
    double getZ();

    RoutingProtocol getRoutingProtocol();

    void onApplicationPacketReceived(NetworkPacket packet);

    ApplicationMessageBus getApplicationBus();

    Vec3 getPos();
}
