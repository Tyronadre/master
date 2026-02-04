package de.tyro.mcnetwork.routing;

import de.tyro.mcnetwork.routing.packet.IApplicationPaket;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.protocol.RoutingProtocol;
import net.minecraft.world.phys.Vec3;

public interface INetworkNode {

    IP getIP();

    double getX();
    double getY();
    double getZ();

    /**
     * @return the routing protocol that this computer currently uses
     */
    RoutingProtocol getRoutingProtocol();

    /**
     * @return the bus where application packages can be accessed for any application running on this computer
     */
    ApplicationMessageBus getApplicationBus();

    /**
     * @return the center position of this block
     */
    Vec3 getPos();

    /**
     * Called when a packet is delivered to this node
     */
    void onPacketReceived(INetworkPacket packet);

    /**
     * Called when an application packed should be processed by this node
     */
    void onApplicationPacketReceived(IApplicationPaket packet);

}
