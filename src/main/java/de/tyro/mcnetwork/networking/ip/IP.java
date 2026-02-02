package de.tyro.mcnetwork.networking.ip;

import java.util.Arrays;
import java.util.stream.Collectors;

public class IP {
    int[] address;

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

    @Override
    public String toString() {
        return Arrays.stream(address).mapToObj(String::valueOf).collect(Collectors.joining("."));
    }
}
