package de.tyro.mcnetwork.simulation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Arrays;
import java.util.stream.Collectors;

public class IP implements INBTSerializable<CompoundTag> {

    public static final IP ZERO = new IP(new int[]{0, 0, 0, 0});
    public static final IP BROADCAST = new IP(new int[]{255, 255, 255, 255});
    int[] address;

    public IP() {
        this.address = new int[]{0, 0, 0, 0};
    }

    public IP(int[] address) {
        if (address == null || address.length != 4) throw new IllegalArgumentException("Invalid IP address: " + Arrays.toString(address));
        this.address = address;

    }

    public IP(String address) {
        if (address == null) throw new IllegalArgumentException("Invalid IP address: null");
        var split = address.split("\\.");
        if (split.length != 4) throw new IllegalArgumentException("Invalid IP address: " + address);

        this.address = new int[4];
        for (int i = 0; i < split.length; i++) this.address[i] = Integer.parseInt(split[i]);

    }

    public static boolean validateIp(String ip) {
        return ip.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    }



    public int[] asArray() {
        return address;
    }

    @Override
    public String toString() {
        return Arrays.stream(address).mapToObj(String::valueOf).collect(Collectors.joining("."));
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof IP ip)) return false;

        return Arrays.equals(address, ip.address);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(address);
    }


    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var tag = new CompoundTag(4);
        for (int i = 0; i < address.length; i++) tag.putInt(String.valueOf(i), address[i]);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
        for (int i = 0; i < 4; i++) address[i] = nbt.getInt(String.valueOf(i));
    }

    public int compareTo(IP neighborAddress) {
        return hashCode() - neighborAddress.hashCode();
    }
}
