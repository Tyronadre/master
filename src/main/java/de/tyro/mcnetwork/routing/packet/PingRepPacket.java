package de.tyro.mcnetwork.routing.packet;

import de.tyro.mcnetwork.routing.IP;

import java.util.StringJoiner;
import java.util.UUID;

public class PingRepPacket extends NetworkPacket implements IApplicationPaket {
    public final long sendTime;
    public final long returnTime;
    public final UUID replyUUID;

    public PingRepPacket(IP sourceIp, IP destinationIp, long sendTime, long returnTime, UUID replyUUID) {
        super(sourceIp, destinationIp);
        this.sendTime = sendTime;
        this.returnTime = returnTime;
        this.replyUUID = replyUUID;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PingRepPacket.class.getSimpleName() + "[", "]").add("sendTime=" + sendTime).add("returnTime=" + returnTime).add("id=" + id).add("sourceIp=" + sourceIp).add("destinationIp=" + destinationIp).toString();
    }
}
