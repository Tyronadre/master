package de.tyro.mcnetwork.block.entity;

import de.tyro.mcnetwork.routing.ApplicationMessageBus;
import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.routing.packet.NetworkPacket;
import de.tyro.mcnetwork.routing.packet.PingPacket;
import de.tyro.mcnetwork.routing.packet.PingRepPacket;
import de.tyro.mcnetwork.routing.protocol.AODVProtocol;
import de.tyro.mcnetwork.routing.protocol.RoutingProtocol;
import de.tyro.mcnetwork.terminal.Terminal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class ComputerBlockEntity extends BlockEntity implements INetworkNode {
    //server
    private IP ipAddress;
    private RoutingProtocol routingProtocol;

    //client
    private Terminal terminal;
    private ApplicationMessageBus applicationMessageBus;

    public ComputerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.COMPUTER_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        if (level == null) return;

        if (level.isClientSide) {
            ipAddress = IP.getNextFreeIP();
            routingProtocol = new AODVProtocol();
            SimulationEngine.INSTANCE.registerNode(this);
//        } else {
            terminal = new Terminal(this);
            applicationMessageBus = new ApplicationMessageBus();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!level.isClientSide) {
            SimulationEngine.INSTANCE.unregisterNode(this);
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
    public void onApplicationPacketReceived(NetworkPacket packet) {
        if (packet instanceof PingPacket ping) {
            getRoutingProtocol().sendData(this, new PingRepPacket(getIP(), ping.sourceIp, ping.sendTime, SimulationEngine.INSTANCE.getSimTime(), ping.id));
            return;
        }

        //relay to the message bus, where other apps can handle it
        applicationMessageBus.handle(packet);
    }

    @Override
    public ApplicationMessageBus getApplicationBus() {
        return applicationMessageBus;
    }

    public List<String> getRenderText() {
        var list = new ArrayList<String>();
        if (getIP() != null) list.add(getIP().toString());
        if (getRoutingProtocol() != null) list.addAll(getRoutingProtocol().renderData());

        return list;
    }
}
