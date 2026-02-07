package de.tyro.mcnetwork.routing;

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

    public static boolean validateIp(String ip) {
        return ip.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    }

    public static IP getNextFreeIP() {
        int[] ip = new int[4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 255; j++) {
                ip[3 - i] = j;
                var ipO = new IP(ip);
                if (SimulationEngine.INSTANCE.getNodeList().stream().noneMatch(it -> it.getIP().equals(ipO)))
                    return ipO;
            }
        }
        throw new IllegalStateException("No free IP address found!");
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
}
