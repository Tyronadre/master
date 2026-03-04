package de.tyro.mcnetwork.network.payload.networkPacket;


import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.routing.packet.aodv.AODVRREPPacket;
import de.tyro.mcnetwork.routing.packet.aodv.AODVRREQPacket;
import de.tyro.mcnetwork.routing.packet.application.DestinationUnreachablePacket;
import de.tyro.mcnetwork.routing.packet.application.PingPacket;
import de.tyro.mcnetwork.routing.packet.application.PingRepPacket;
import de.tyro.mcnetwork.routing.packet.dsr.DSRRouteReply;
import de.tyro.mcnetwork.routing.packet.dsr.DSRRouteRequest;
import de.tyro.mcnetwork.routing.packet.dsr.DSRSourceRoute;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;

import static de.tyro.mcnetwork.network.payload.networkPacket.NetworkPacketCodecRegistry.register;

public class NetworkPacketCodecGenerator {
    public static void generate() {
        register(DestinationUnreachablePacket.class,
                (buf, uuid, originatorIP, destinationIP) -> new DestinationUnreachablePacket(originatorIP, destinationIP),
                (buf, packet) -> {
                }
        );

        register(PingPacket.class,
                (buf, uuid, originatorIP, destinationIP) -> new PingPacket(uuid, originatorIP, destinationIP, SimulationEngine.getInstance(false).getSimTime()),
                (buf, packet) -> {
                },
                (PingPacket packet, IPayloadContext context) -> {
                    packet.sendStartTime = SimulationEngine.getInstance(context.flow().isClientbound()).getSimTime();
                }
        );

        register(PingRepPacket.class,
                (buf, uuid, ip1, ip2) -> new PingRepPacket(
                        uuid,
                        ip1,
                        ip2,
                        buf.readLong(),
                        buf.readDouble(),
                        buf.readUUID()),
                (buf, packet) -> buf
                        .writeLong(packet.sendTime)
                        .writeDouble(packet.returnStartTime)
                        .writeUUID(packet.replyUUID));

        register(AODVRREQPacket.class,
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

        register(AODVRREPPacket.class,
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

        register(DSRRouteRequest.class,
                (buf, uuid, originatorIP, destinationIP) -> new DSRRouteRequest(
                        uuid,
                        originatorIP,
                        destinationIP,
                        buf.readInt(),
                        buf.readIP(),
                        buf.readIPList()
                ),
                (buf, packet) -> buf
                        .writeInt(packet.getIdentificationValue())
                        .writeIP(packet.getTargetAddress())
                        .writeIPList(new ArrayList<>(packet.getAddresses())
                        ));

        register(DSRRouteReply.class,
                (buf, uuid, originatorIP, destinationIP) -> new DSRRouteReply(
                        uuid,
                        originatorIP,
                        destinationIP,
                        buf.readBoolean(),
                        buf.readIPList()
                ),
                (buf, packet) -> buf
                        .writeBoolean(packet.getLastHopExternalFlag())
                        .writeIPList(new ArrayList<>(packet.getAddresses())
                        ));

        register(DSRSourceRoute.class,
                (buf, uuid, originatorIP, destinationIP) -> new DSRSourceRoute(
                        uuid,
                        originatorIP,
                        destinationIP,
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readInt(),
                        buf.readIPList(),
                        buf.readPacket()
                ),
                (buf, packet) -> buf
                        .writeBoolean(packet.getFirstHopExternalFlag())
                        .writeBoolean(packet.getLastHopExternalFlag())
                        .writeInt(packet.getSegLeft())
                        .writeIPList(packet.getAddresses())
                        .writePacket(packet.getPacket()));
    }


}
