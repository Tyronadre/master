package de.tyro.mcnetwork.block.entity;

import com.mojang.logging.LogUtils;
import de.tyro.mcnetwork.entity.NetworkFrameEntity;
import de.tyro.mcnetwork.network.payload.NewNetworkPacketPayload;
import de.tyro.mcnetwork.simulation.ApplicationMessageBus;
import de.tyro.mcnetwork.simulation.INetworkNode;
import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.SimulationEngine;
import de.tyro.mcnetwork.simulation.packet.IApplicationPacket;
import de.tyro.mcnetwork.simulation.packet.IProtocolPaket;
import de.tyro.mcnetwork.simulation.packet.application.PingPacket;
import de.tyro.mcnetwork.simulation.packet.application.PingRepPacket;
import de.tyro.mcnetwork.simulation.packet.application.TraceRoutePacket;
import de.tyro.mcnetwork.simulation.packet.application.TraceRouteReplyPacket;
import de.tyro.mcnetwork.simulation.protocol.IRoutingProtocol;
import de.tyro.mcnetwork.terminal.Terminal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ComputerBlockEntity extends BlockEntity implements INetworkNode {
    private static final Logger log = LogUtils.getLogger();
    private IRoutingProtocol routingProtocol;

    //client
    private Terminal terminal;
    private ApplicationMessageBus applicationMessageBus;
    private ReceiveWindow receiveWindow;
    //server
    private IP ipAddress = IP.ZERO;

    public ComputerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.COMPUTER_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        log.debug("onLoad");
        if (level == null) return;
        var sim = SimulationEngine.getInstance(level.isClientSide);

        terminal = new Terminal(this);
        applicationMessageBus = new ApplicationMessageBus(this);
        receiveWindow = new ReceiveWindow(sim.getReceiveWindowMS(), sim.getReceiveWindowSize());

        if (!ipAddress.equals(IP.ZERO))
            SimulationEngine.getInstance(level.isClientSide).registerNode(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level.isClientSide()) {
            if (terminal != null) terminal.interrupt();
        }

        SimulationEngine.getInstance(level.isClientSide).unregisterNode(this);
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
    public IRoutingProtocol getRoutingProtocol() {
        return routingProtocol;
    }

    @Override
    public void onApplicationPacketReceived(IApplicationPacket packet) {
        var sim = SimulationEngine.getInstance(level.isClientSide);

        if (packet instanceof PingPacket ping) {
            routingProtocol.send(new PingRepPacket(getIP(), ping.getOriginatorIP(), (int) (sim.getSimTime() - ping.sendStartTime), sim.getSimTime(), ping.id), Integer.MAX_VALUE);
            return;
        }

        if (packet instanceof TraceRoutePacket traceRoute) {
            routingProtocol.send(new TraceRouteReplyPacket(getIP(), traceRoute.getOriginatorIP(), (int) (sim.getSimTime() - traceRoute.sendStartTime), sim.getSimTime(), traceRoute.id, 0), Integer.MAX_VALUE);
            return;
        }

        //All the packets that are for the client are packets that will be processed by the applications on this Computer. Therefor we send them to the internal application bus.
        if (!level.isClientSide && packet instanceof PingRepPacket pingRep) {
            NewNetworkPacketPayload.sendToSelf(packet, this);
        }

        if (!level.isClientSide && packet instanceof TraceRouteReplyPacket traceRep) {
            NewNetworkPacketPayload.sendToSelf(packet, this);
        }

        if (level.isClientSide) {
            applicationMessageBus.handle(packet);
        }
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
    public void onFrameReceive(NetworkFrameEntity frame) {
        if (!receiveWindow.tryAccept()) {
            frame.setInterfered();
            return;
        }

        var packet = frame.getPacket();

        if (packet instanceof IProtocolPaket pp)
            routingProtocol.onProtocolPacketReceived(pp);
        else if (!level.isClientSide && packet instanceof IApplicationPacket ap && packet.getDestinationIP().equals(this.ipAddress))
            this.onApplicationPacketReceived(ap);
        else if (!level.isClientSide && packet instanceof TraceRoutePacket traceRoute && frame.getTtl() == 1) {
            // TTL expired at this node, send TraceRouteReply
            var sim = SimulationEngine.getInstance(level.isClientSide);
            routingProtocol.send(new TraceRouteReplyPacket(getIP(), traceRoute.getOriginatorIP(), (int) (sim.getSimTime() - traceRoute.sendStartTime), sim.getSimTime(), traceRoute.id, 0), Integer.MAX_VALUE);
        } else if (frame.getTtl() > 0 && !packet.getDestinationIP().equals(this.ipAddress))
            routingProtocol.send(packet, frame.getTtl() - 1);
    }

    public List<String> getRenderText() {
        var list = new ArrayList<String>();
        if (getIP() != null) list.add(getIP().toString());

        return list;
    }


    @Override
    public void simTick() {
        if (level.isClientSide()) {
            applicationMessageBus.simTick();
        }
        routingProtocol.simTick();
    }

    @Override
    public double distanceTo(@NotNull INetworkNode to) {
        return this.getPos().distanceTo(to.getPos());
    }


    @Override
    public void sendApplicationPacket(IApplicationPacket packet, int ttl) {
        if (level == null) return;

        if (level.isClientSide()) PacketDistributor.sendToServer(new NewNetworkPacketPayload(packet, ttl, getBlockPos(), false));

        if (packet.getDestinationIP().equals(ipAddress)) onApplicationPacketReceived(packet);

        routingProtocol.send(packet, ttl);
    }

    @Override
    public void setProtocol(IRoutingProtocol routingProtocol) {
        this.routingProtocol = routingProtocol;
    }

    @Override
    public void onServerStop() {
        this.terminal.interrupt();
    }

    @Override
    public void setIP(IP ip) {
        this.ipAddress = ip;
    }

    class ReceiveWindow {
        private final long windowSizeMs;
        private final int maxPacketsPerWindow;

        private long windowStartTick = -1;
        private int count = 0;

        public ReceiveWindow(long windowSizeMs, int maxPacketsPerWindow) {
            this.windowSizeMs = windowSizeMs;
            this.maxPacketsPerWindow = maxPacketsPerWindow;
        }

        public boolean tryAccept() {
            var sim = SimulationEngine.getInstance(level.isClientSide);
            if (!sim.getReceiveWindowActive()) return true;

            var currentTick = sim.getSimTime();

            if (windowStartTick == -1 || currentTick - windowStartTick >= windowSizeMs) {
                windowStartTick = currentTick;
                count = 0;
            }

            count++;
            return count <= maxPacketsPerWindow;
        }
    }


    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        log.debug("loadAdditional");

        super.loadAdditional(tag, registries);
        ipAddress = new IP();
        ipAddress.deserializeNBT(registries, tag.getCompound("ip"));
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        log.debug("saveAdditional");

        super.saveAdditional(tag, registries);
        tag.put("ip", ipAddress.serializeNBT(registries));
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        log.debug("getUpdatePacket");
        if (level != null && !level.isClientSide && ipAddress.equals(IP.ZERO)) {
            ipAddress = SimulationEngine.getNextFreeIP();
        }
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        log.debug("onDataPacket");
        if (this.level != null && this.level.isClientSide) {
            super.onDataPacket(net, pkt, lookupProvider);
            SimulationEngine.getInstance(true).registerNode(this);
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        log.debug("getUpdateTag");


        return saveWithoutMetadata(registries);
    }

    @Override
    public String toString() {
        return "Computer: " + getIP() + "@" + Integer.toHexString(hashCode());
    }
}
