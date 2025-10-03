package de.tyro.mcnetwork.networking;

public class IPPacket {
    public String srcIp;
    public String dstIp;
    public String protocol; // "TCP", "UDP", "ICMP"
    public byte[] payload;

    public IPPacket(String srcIp, String dstIp, String protocol, byte[] payload) {
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.protocol = protocol;
        this.payload = payload;
    }
}

