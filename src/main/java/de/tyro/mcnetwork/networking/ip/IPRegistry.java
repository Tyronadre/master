package de.tyro.mcnetwork.networking.ip;

import de.tyro.mcnetwork.block.entity.ComputerBlockEntity;

import java.util.concurrent.ConcurrentHashMap;

public class IPRegistry {
    private static ConcurrentHashMap<ComputerBlockEntity, IP> blockToIP = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<IP, ComputerBlockEntity> ipToBlock = new ConcurrentHashMap<>();


    public static ComputerBlockEntity getByIP(IP ip) {
        return ipToBlock.get(ip);
    }

    public static IP getByComputer(ComputerBlockEntity computer) {
        return blockToIP.get(computer);
    }

    public static IP getNextFreeIP() {
        int[] ip = new int[4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 129; j++) {
                ip[i] = j;
                var ipO = new IP(ip);
                if (!ipToBlock.containsKey(ipO)) return ipO;
            }
        }
        throw new IllegalStateException("No free IP address found!");
    }

    public static boolean isIpFree(IP ip) {
        return ipToBlock.containsKey(ip);
    }

    public static boolean validateIp(String ip) {
        return ip.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    }

    public static void register(ComputerBlockEntity computerBlockEntity) {
        blockToIP.put(computerBlockEntity, computerBlockEntity.getIpAddress());
        ipToBlock.put(computerBlockEntity.getIpAddress(), computerBlockEntity);
    }
}
