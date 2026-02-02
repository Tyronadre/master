package de.tyro.mcnetwork.block.entity;

import de.tyro.mcnetwork.gui.TerminalMenu;
import de.tyro.mcnetwork.networking.EthernetFrame;
import de.tyro.mcnetwork.networking.IPPacket;
import de.tyro.mcnetwork.networking.NetworkUtils;
import de.tyro.mcnetwork.networking.ip.IP;
import de.tyro.mcnetwork.networking.ip.IPRegistry;
import de.tyro.mcnetwork.routing.core.SimulationRegistry;
import de.tyro.mcnetwork.routing.node.SimNode;
import de.tyro.mcnetwork.routing.protocol.AodvProtocol;
import de.tyro.mcnetwork.terminal.Terminal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ComputerBlockEntity extends BlockEntity {
    private String macAddress;
    private IP ipAddress;
    private final SimNode node;
    private final Terminal terminal;

    // Statische Registry für IP/MAC-Adressen
    private static final ConcurrentHashMap<UUID, ComputerBlockEntity> macRegistry = new ConcurrentHashMap<>();

    public ComputerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.COMPUTER_BE.get(), pos, state);
        this.macAddress = NetworkUtils.generateMAC();
        this.ipAddress = IPRegistry.getNextFreeIP();
        this.node = new SimNode(
                UUID.randomUUID(),
                null,
                pos,
                new AodvProtocol()
        );
        this.terminal = new Terminal();
        SimulationRegistry.getEngine().addNode(node);
        IPRegistry.register(this);
        macRegistry.put(node.getId(), this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("macAddress", this.macAddress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.macAddress = tag.getString("macAddress");
    }

    public void receivePacket(EthernetFrame frame) {
        // Decode as IP packet
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(frame.payload);
            ObjectInputStream ois = new ObjectInputStream(bis);
            IPPacket ip = (IPPacket) ois.readObject();

            if (ip.dstIp.equals(this.ipAddress)) {
                System.out.println("Computer " + this.ipAddress + " received packet from " + ip.srcIp);
                // TODO: feed result into terminal output
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static UUID getMacByIp(IP ip) {
        ComputerBlockEntity entity = IPRegistry.getByIP(ip);
        return entity != null ? entity.node.getId() : null;
    }

    public static ComputerBlockEntity getByMac(UUID mac) {
        return macRegistry.get(mac);
    }

    public void setIpAddress(IP ip) {
        IPRegistry.register(this);
        this.ipAddress = ip;
    }

    public void testConnection(IP zielIp) {
        UUID zielMac = getMacByIp(zielIp);
        if (zielMac == null) {
            System.err.println("Keine MAC-Adresse für IP " + zielIp + " gefunden!");
            return;
        }
        // Beispiel: 5 Pakete im Abstand von 5 Ticks
        for (int i = 0; i < 5; i++) {
            SimulationRegistry.getEngine().enqueueTestPacket(node, zielMac, i * 5);
        }
    }

    public void open()  {

    }

    public SimNode getNode() {
        return node;
    }

    public List<String> getRenderText() {
        return List.of("Computer Node",
                node.getId().toString(),
                node.getProtocol().getType().toString());
    }

    public String getMacAddress() {
        return macAddress;
    }

    public IP getIpAddress() {
        return ipAddress;
    }

    public Terminal getTerminal() {
        return terminal;
    }
}
