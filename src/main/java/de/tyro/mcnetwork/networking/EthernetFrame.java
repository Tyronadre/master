package de.tyro.mcnetwork.networking;

public class EthernetFrame {
    public String srcMac;
    public String dstMac;
    public byte[] payload;

    public EthernetFrame(String srcMac, String dstMac, byte[] payload) {
        this.srcMac = srcMac;
        this.dstMac = dstMac;
        this.payload = payload;
    }
}
