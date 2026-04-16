package de.tyro.mcnetwork.sniffer;


import de.tyro.mcnetwork.simulation.IP;
import de.tyro.mcnetwork.simulation.packet.INetworkPacket;

public class CapturedFrame {
    private final long timestamp;
    private final IP frameFrom;
    private final IP frameTo;
    private final IP packetOriginator;
    private final IP packetDestination;
    private final Class<?> packetType;
    private final INetworkPacket packet;

    public CapturedFrame(long timestamp, IP frameFrom, IP frameTo, INetworkPacket packet) {
        this.timestamp = timestamp;
        this.frameFrom = frameFrom;
        this.frameTo = frameTo;
        this.packetOriginator = packet.getOriginatorIP();
        this.packetDestination = packet.getDestinationIP();
        this.packetType = packet.getClass();
        this.packet = packet.copy(); // Assuming copy() creates a deep copy
    }

    public long getTimestamp() {
        return timestamp;
    }

    public IP getFrameFrom() {
        return frameFrom;
    }

    public IP getFrameTo() {
        return frameTo;
    }

    public IP getPacketOriginator() {
        return packetOriginator;
    }

    public IP getPacketDestination() {
        return packetDestination;
    }

    public Class<?> getPacketType() {
        return packetType;
    }

    public INetworkPacket getPacket() {
        return packet;
    }
}
