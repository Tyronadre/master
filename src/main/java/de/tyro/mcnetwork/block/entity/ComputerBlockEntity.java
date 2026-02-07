package de.tyro.mcnetwork.block.entity;

import de.tyro.mcnetwork.routing.ApplicationMessageBus;
import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.routing.packet.IApplicationPaket;
import de.tyro.mcnetwork.routing.packet.INetworkPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.PingPacket;
import de.tyro.mcnetwork.routing.packet.PingRepPacket;
import de.tyro.mcnetwork.routing.protocol.AODVProtocol;
import de.tyro.mcnetwork.routing.protocol.RoutingProtocol;
import de.tyro.mcnetwork.terminal.Terminal;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ComputerBlockEntity extends BlockEntity implements INetworkNode {
    //server
    private IP ipAddress;
    private RoutingProtocol routingProtocol;

    //client
    private Terminal terminal;
    private ApplicationMessageBus applicationMessageBus;
    private ReceiveWindow receiveWindow;
    private Deque<PingPacket> scheduledPackages;

    public ComputerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.COMPUTER_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        if (level == null) return;

        if (level.isClientSide) {
            ipAddress = IP.getNextFreeIP();
            routingProtocol = new AODVProtocol(this);
            SimulationEngine.INSTANCE.registerNode(this);
//        } else {
            terminal = new Terminal(this);
            applicationMessageBus = new ApplicationMessageBus();
            receiveWindow = new ReceiveWindow(5, 1);
            scheduledPackages = new ConcurrentLinkedDeque<>();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level.isClientSide) {
            SimulationEngine.INSTANCE.unregisterNode(this);
            terminal.interrupt();
        }
    }

    public IP getIP() {
        return ipAddress;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    @Override
    public double getX() {
        return getBlockPos().getX();
    }

    @Override
    public double getY() {
        return getBlockPos().getY();
    }

    @Override
    public double getZ() {
        return getBlockPos().getZ();
    }

    @Override
    public RoutingProtocol getRoutingProtocol() {
        return routingProtocol;
    }

    @Override
    public void onApplicationPacketReceived(IApplicationPaket packet) {
        if (packet instanceof PingPacket ping) {
            routingProtocol.unicast(new PingRepPacket(getIP(), ping.originatorIP, SimulationEngine.INSTANCE.getSimTime() - ping.sendStartTime, SimulationEngine.INSTANCE.getSimTime(), ping.id));
            return;
        }

        //relay to the message bus, where other apps can handle it
        applicationMessageBus.handle(packet);
    }

    @Override
    public void unicast(PingPacket packet) {
        if (!Minecraft.getInstance().isSameThread()) this.scheduledPackages.add(packet);
        else routingProtocol.unicast(packet);
    }

    @Override
    public ApplicationMessageBus getApplicationBus() {
        return applicationMessageBus;
    }

    @Override
    public Vec3 getPos() {
        return Vec3.atCenterOf(getBlockPos());
    }

    @Override
    public void onPacketReceived(INetworkPacket packet) {
//        if (!receiveWindow.tryAccept()) {
//            System.out.println("Received packet but not accepted by the client, interferred");
//            return;
//        }

        if (packet instanceof IProtocolPaket pp) routingProtocol.onProtocolPacketReceived(pp);
        else if (packet instanceof IApplicationPaket ap && packet.getDestinationIP().equals(this.ipAddress)) this.onApplicationPacketReceived(ap);
        else routingProtocol.unicast(packet);
    }

    public List<String> getRenderText() {
        var list = new ArrayList<String>();
        if (getIP() != null) list.add(getIP().toString());
        if (getRoutingProtocol() != null) list.addAll(getRoutingProtocol().renderData());

        return list;
    }




    @Override
    public void tick() {
        this.scheduledPackages.forEach(p -> routingProtocol.unicast(p));
        scheduledPackages.clear();

        routingProtocol.tick();
        applicationMessageBus.tick();

    }

    @Override
    public double distanceTo(INetworkNode to) {
        return this.getPos().distanceTo(to.getPos());
    }

    static class ReceiveWindow {
        private final long windowTicks;
        private final int maxPackets;

        private long windowStartTick = -1;
        private int count = 0;

        public ReceiveWindow(long windowTicks, int maxPackets) {
            this.windowTicks = windowTicks;
            this.maxPackets = maxPackets;
        }

        public boolean tryAccept() {
            var currentTick = SimulationEngine.INSTANCE.getSimTime();

            if (windowStartTick == -1 || currentTick - windowStartTick >= windowTicks) {
                windowStartTick = currentTick;
                count = 0;
            }

            count++;
            return count <= maxPackets;
        }
    }


}

