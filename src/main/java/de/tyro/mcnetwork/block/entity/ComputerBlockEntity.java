package de.tyro.mcnetwork.block.entity;

import de.tyro.mcnetwork.block.BlockRegistry;
import de.tyro.mcnetwork.gui.ComputerMenu;
import de.tyro.mcnetwork.networking.EthernetFrame;
import de.tyro.mcnetwork.networking.IPPacket;
import de.tyro.mcnetwork.networking.NetworkUtils;
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

public class ComputerBlockEntity extends BlockEntity implements MenuProvider {
    private String macAddress;
    private String ipAddress;

    public ComputerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.COMPUTER_BE.get(), pos, state);
        this.macAddress = NetworkUtils.generateMAC();
        this.ipAddress = "0.0.0.0";
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Computer");
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

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ComputerMenu(id, inv, this);
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


    public String getMacAddress() {
        return macAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ip) {
        this.ipAddress = ip;
    }
}
