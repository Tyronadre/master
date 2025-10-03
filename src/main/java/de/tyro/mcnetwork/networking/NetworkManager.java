package de.tyro.mcnetwork.networking;

import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;

import java.util.HashMap;
import java.util.Map;

public class NetworkManager {
    private static final Map<String, ComputerBlockEntity> devices = new HashMap<>();

    public static void registerDevice(String mac, ComputerBlockEntity device) {
        devices.put(mac, device);
    }

    public static void unregisterDevice(String mac) {
        devices.remove(mac);
    }

    public static void sendPacket(EthernetFrame frame) {
        ComputerBlockEntity dst = devices.get(frame.dstMac);
        if (dst != null) {
            dst.receivePacket(frame);
        } else {
            System.out.println("Packet dropped: destination not found.");
        }
    }
}

