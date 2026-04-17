package de.tyro.mcnetwork.simulation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class IP implements INBTSerializable<CompoundTag> {

    private static final Set<IP> ips = new HashSet<>();
    public static final IP ZERO = new IP(new int[]{0, 0, 0, 0});
    public static final IP BROADCAST = new IP(new int[]{255, 255, 255, 255});
    int[] address;

    public IP() {
        this.address = new int[]{0, 0, 0, 0};
    }

    public IP(int[] address) {
        if (address == null || address.length != 4) throw new IllegalArgumentException("Invalid IP address: " + Arrays.toString(address));
        this.address = address;

        ips.add(this);
    }

    public IP(String address) {
        if (address == null) throw new IllegalArgumentException("Invalid IP address: null");
        var split = address.split("\\.");
        if (split.length != 4) throw new IllegalArgumentException("Invalid IP address: " + address);

        this.address = new int[4];
        for (int i = 0; i < split.length; i++) this.address[i] = Integer.parseInt(split[i]);

        ips.add(this);
    }

    public static boolean validateIp(String ip) {
        return ip.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    }

    public static IP getNextFreeIP() {
        int[] ip = new int[4];
        for (int i = 0; i < 4; i++) {
            for (int j = i; j >= 0; j--) ip[j] = 0;
            for (int j = 0; j < 255; j++) {
                ip[3 - i] = j;

                if (Arrays.equals(ip, ZERO.address) || ip.equals(BROADCAST.address)) {
                    continue;
                }
                if (ips.stream().noneMatch(it -> Arrays.equals(ip, it.address))) return new IP(ip);
            }
        }
        throw new IllegalStateException("No free IP address found!");
    }

    public static void freeAddress(IP ip) {
        ips.remove(ip);
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
        ips.add(this);
    }

    public int compareTo(IP neighborAddress) {
        return hashCode() - neighborAddress.hashCode();
    }
}
