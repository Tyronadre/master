package de.tyro.mcnetwork.networking;

import java.util.Random;

public class NetworkUtils {
    private static final Random rand = new Random();

    public static String generateMAC() {
        byte[] macAddr = new byte[6];
        rand.nextBytes(macAddr);
        macAddr[0] = (byte)(macAddr[0] & (byte)254); // unicast, not multicast
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < macAddr.length; i++) {
            sb.append(String.format("%02X%s", macAddr[i], (i < macAddr.length - 1) ? ":" : ""));
        }
        return sb.toString();
    }

    public static boolean validateIp(String ip) {
        return ip.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    }
}
