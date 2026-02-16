package de.tyro.mcnetwork.routing;

import de.tyro.mcnetwork.entity.NetworkFrameEntity;
import de.tyro.mcnetwork.routing.packet.IApplicationPaket;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.protocol.IRoutingProtocol;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public interface INetworkNode {

    IP getIP();

    double getX();

    double getY();

    double getZ();

    /**
     * @return the routing protocol that this computer currently uses
     */
    IRoutingProtocol getRoutingProtocol();

    /**
     * @return the bus where application packages can be accessed for any application running on this computer
     */
    ApplicationMessageBus getApplicationBus();

    /**
     * @return the center position of this block
     */
    Vec3 getPos();

    BlockPos getBlockPos();

    /**
     * Called when a frame is delivered to this node. This should only be called from the server thread
     */
    void onFrameReceive(NetworkFrameEntity packet);

    /**
     * Called when an application packed should be processed by this node
     */
    void onApplicationPacketReceived(IApplicationPaket packet);

    void tick();

    /**
     * Calculates the euclidean distance to another node
     *
     * @param to the other node
     * @return the distance
     */
    double distanceTo(@NotNull INetworkNode to);

    /**
     * Routes a packet to the destination address of it
     *
     * @param packet the packet to send
     * @param ttl    the time to life for frame that will be send
     */
    void send(INetworkPacket packet, int ttl);

    Level getLevel();
}
