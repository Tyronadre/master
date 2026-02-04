package de.tyro.mcnetwork.routing.packet;


import de.tyro.mcnetwork.routing.IP;

import java.util.List;
import java.util.StringJoiner;

public class PingPacket extends NetworkPacket implements IApplicationPaket {

    public final long sendTime;

    public PingPacket(IP src, IP dst, long sendTime) {
        super(src, dst);
        this.sendTime = sendTime;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PingPacket.class.getSimpleName() + "[", "]")
                .add("sendTime=" + sendTime)
                .add("id=" + id)
                .add("sourceIp=" + sourceIp)
                .add("destinationIp=" + destinationIp)
                .toString();
    }

    @Override
    public List<String> getRenderContent() {
        return List.of(toString().split(","));
    }
}
