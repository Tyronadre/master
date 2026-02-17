package de.tyro.mcnetwork.network.payload.networkPacket;


import de.tyro.mcnetwork.routing.packet.AODVRREPPacket;
import de.tyro.mcnetwork.routing.packet.AODVRREQPacket;
import de.tyro.mcnetwork.routing.packet.DestinationUnreachablePacket;
import de.tyro.mcnetwork.routing.packet.PingPacket;
import de.tyro.mcnetwork.routing.packet.PingRepPacket;

public class NetworkPacketCodecGenerator {
    public static void generate() {
        NetworkPacketCodecRegistry.register(DestinationUnreachablePacket.class,
                (buf, uuid, originatorIP, destinationIP) -> new DestinationUnreachablePacket(originatorIP, destinationIP),
                (buf, packet) -> {}
        );

        NetworkPacketCodecRegistry.register(PingPacket.class,
                (buf, uuid, originatorIP, destinationIP) -> new PingPacket(uuid, originatorIP, destinationIP, buf.readLong()),
                (buf, packet) -> buf.writeLong(packet.sendStartTime)
        );

        NetworkPacketCodecRegistry.register(PingRepPacket.class,
                (buf, uuid, ip1, ip2) -> new PingRepPacket(
                        uuid,
                        ip1,
                        ip2,
                        buf.readLong(),
                        buf.readLong(),
                        buf.readUUID()),
                (buf, packet) -> buf
                        .writeLong(packet.sendTime)
                        .writeLong(packet.returnStartTime)
                        .writeUUID(packet.replyUUID));

        NetworkPacketCodecRegistry.register(AODVRREQPacket.class,
                (buf, uuid, oIP, dIP) -> new AODVRREQPacket(
                        uuid,
                        oIP,
                        dIP,
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readInt()),
                (buf, packet) -> buf
                        .writeBoolean(packet.joinFlag)
                        .writeBoolean(packet.repairFlag)
                        .writeBoolean(packet.gratuitousFlag)
                        .writeBoolean(packet.destinationOnlyFlag)
                        .writeBoolean(packet.unknownSeqFlag)
                        .writeInt(packet.hopCount)
                        .writeInt(packet.rreqId)
                        .writeInt(packet.destinationSequenceNumber)
                        .writeInt(packet.originatorSequenceNumber));

        NetworkPacketCodecRegistry.register(AODVRREPPacket.class,
                (buf, uuid, originatorIP, destinationIP) -> new AODVRREPPacket(
                        uuid,
                        originatorIP,
                        destinationIP,
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readInt(),
                        buf.readInt(),
                        buf.readLong()),
                (buf, packet) -> buf.writeBoolean(packet.repairFlag)
                        .writeBoolean(packet.ackRequiredFlag)
                        .writeInt(packet.hopCount)
                        .writeInt(packet.destSeqNumber)
                        .writeLong(packet.lifetime));


    }


}
