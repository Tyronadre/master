package de.tyro.mcnetwork.block.entity;

import de.tyro.mcnetwork.entity.NetworkFrameEntity;
import de.tyro.mcnetwork.network.payload.NewNetworkPacketPayload;
import de.tyro.mcnetwork.routing.ApplicationMessageBus;
import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.routing.packet.IApplicationPacket;
import de.tyro.mcnetwork.routing.packet.IProtocolPaket;
import de.tyro.mcnetwork.routing.packet.application.PingPacket;
import de.tyro.mcnetwork.routing.packet.application.PingRepPacket;
import de.tyro.mcnetwork.routing.protocol.AODVProtocol;
import de.tyro.mcnetwork.routing.protocol.IRoutingProtocol;
import de.tyro.mcnetwork.terminal.Terminal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ComputerBlockEntity extends BlockEntity implements INetworkNode {
    //server
    private IP ipAddress = IP.getNextFreeIP();
    private IRoutingProtocol routingProtocol;

    //client
    private Terminal terminal;
    private ApplicationMessageBus applicationMessageBus;
    private ReceiveWindow receiveWindow;

    public ComputerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.COMPUTER_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        if (level == null) return;

        terminal = new Terminal(this);
        applicationMessageBus = new ApplicationMessageBus(this);
        routingProtocol = new AODVProtocol(this);
        SimulationEngine.getInstance(level.isClientSide).registerNode(this);
        receiveWindow = new ReceiveWindow(5, 1);
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
            routingProtocol.send(new PingRepPacket(getIP(), ping.getOriginatorIP(), (long) (sim.getSimTime() - ping.sendStartTime), sim.getSimTime(), ping.id), Integer.MAX_VALUE);
            return;
        }


        //All the packets that are for the client are packets that will be processed by the applications on this Computer. Therefor we send them to the internal application bus.
        if (!level.isClientSide && packet instanceof PingRepPacket pingRep) {
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
//        if (!receiveWindow.tryAccept()) {
//            System.out.println("Received packet but not accepted by the client, interferred");
//            return;
//        }

        var packet = frame.getPacket();

        if (packet instanceof IProtocolPaket pp)
            routingProtocol.onProtocolPacketReceived(pp);
        else if (!level.isClientSide && packet instanceof IApplicationPacket ap && packet.getDestinationIP().equals(this.ipAddress))
            this.onApplicationPacketReceived(ap);
        else if (frame.getTtl() > 0 && !packet.getDestinationIP().equals(this.ipAddress))
            routingProtocol.send(packet, frame.getTtl() - 1);
    }

    public List<String> getRenderText() {
        var list = new ArrayList<String>();
        if (getIP() != null) list.add(getIP().toString());

        return list;
    }


    @Override
    public void tick() {
        if (level.isClientSide()) {
            applicationMessageBus.tick();
        }
        routingProtocol.tick();
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

        if (!level.isClientSide()) routingProtocol.send(packet, ttl);
    }

    @Override
    public void setProtocol(IRoutingProtocol routingProtocol) {
        this.routingProtocol = routingProtocol;
    }

    class ReceiveWindow {
        private final long windowTicks;
        private final int maxPackets;

        private long windowStartTick = -1;
        private int count = 0;

        public ReceiveWindow(long windowTicks, int maxPackets) {
            this.windowTicks = windowTicks;
            this.maxPackets = maxPackets;
        }

        public boolean tryAccept() {
            var currentTick = SimulationEngine.getInstance(level.isClientSide).getSimTime();

            if (windowStartTick == -1 || currentTick - windowStartTick >= windowTicks) {
                windowStartTick = currentTick;
                count = 0;
            }

            count++;
            return count <= maxPackets;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.ipAddress == null) return;
        tag.put("ip", this.ipAddress.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ipAddress.deserializeNBT(registries, tag.getCompound("ip"));

    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public String toString() {
        return "Computer: " + getIP() + "@" + Integer.toHexString(hashCode());
    }
}

