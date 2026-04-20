package de.tyro.mcnetwork.network.payload.networkPacket;


import de.tyro.mcnetwork.network.BetterByteBuf;
import de.tyro.mcnetwork.simulation.SimulationEngine;
import de.tyro.mcnetwork.simulation.packet.aodv.AODVRERRPacket;
import de.tyro.mcnetwork.simulation.packet.aodv.AODVRREPPacket;
import de.tyro.mcnetwork.simulation.packet.aodv.AODVRREQPacket;
import de.tyro.mcnetwork.simulation.packet.application.DestinationUnreachablePacket;
import de.tyro.mcnetwork.simulation.packet.application.PingPacket;
import de.tyro.mcnetwork.simulation.packet.application.PingRepPacket;
import de.tyro.mcnetwork.simulation.packet.application.TraceRoutePacket;
import de.tyro.mcnetwork.simulation.packet.application.TraceRouteReplyPacket;
import de.tyro.mcnetwork.simulation.packet.dsr.DSRRouteError;
import de.tyro.mcnetwork.simulation.packet.dsr.DSRRouteReply;
import de.tyro.mcnetwork.simulation.packet.dsr.DSRRouteRequest;
import de.tyro.mcnetwork.simulation.packet.dsr.DSRSourceRoute;
import de.tyro.mcnetwork.simulation.packet.lar.LARRouteError;
import de.tyro.mcnetwork.simulation.packet.lar.LARRouteReply;
import de.tyro.mcnetwork.simulation.packet.lar.LARRouteRequest;
import de.tyro.mcnetwork.simulation.packet.olsr.OLSRPacket;
import net.minecraft.network.FriendlyByteBuf;

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
                (buf, uuid, originatorIP, destinationIP) -> new PingPacket(uuid, originatorIP, destinationIP, buf.readLong()),
                (buf, packet) -> {
                    buf.writeLong(0);
                }
        );

        register(PingRepPacket.class,
                (buf, uuid, ip1, ip2) -> new PingRepPacket(
                        uuid,
                        ip1,
                        ip2,
                        buf.readInt(),
                        buf.readLong(),
                        buf.readUUID()),
                (buf, packet) -> buf
                        .writeInt(packet.sendTime)
                        .writeLong(packet.returnStartTime)
                        .writeUUID(packet.replyUUID)
        );

        register(TraceRoutePacket.class,
                (buf, uuid, originatorIP, destinationIP) -> new TraceRoutePacket(
                        uuid,
                        originatorIP,
                        destinationIP,
                        0),
                (buf, packet) -> {
                },
                (packet, onClientSide) -> packet.sendStartTime = SimulationEngine.getInstance(onClientSide).getSimTime()
        );

        register(TraceRouteReplyPacket.class,
                (buf, uuid, ip1, ip2) -> new TraceRouteReplyPacket(
                        uuid,
                        ip1,
                        ip2,
                        buf.readInt(),
                        0,
                        buf.readUUID(),
                        buf.readInt()),
                (buf, packet) -> buf
                        .writeInt(packet.sendTime)
                        .writeUUID(packet.replyUUID)
                        .writeInt(packet.hopCount),
                (packet, onClientSide) -> packet.returnStartTime = SimulationEngine.getInstance(onClientSide).getSimTime()
        );

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

        register(AODVRERRPacket.class,
                (buf, uuid, originatorIP, destinationIP) -> new AODVRERRPacket(
                        uuid,
                        originatorIP,
                        destinationIP,
                        buf.readBoolean(),
                        buf.readMap(
                                (buffer) -> BetterByteBuf.IP_STREAM_CODEC.decode(buffer),
                                FriendlyByteBuf::readInt
                        )
                ),
                (buf, packet) -> buf.writeBoolean(packet.noDelete)
                        .writeMap(packet.unreachable,
                                (buffer, value) -> BetterByteBuf.IP_STREAM_CODEC.encode(buffer, value),
                                FriendlyByteBuf::writeInt)
        );

        register(DSRRouteRequest.class,
                (buf, uuid, originatorIP, destinationIP) -> new DSRRouteRequest(
                        uuid,
                        originatorIP,
                        destinationIP,
                        buf.readInt(),
                        buf.readIP(),
                        buf.readIPCollection(ArrayList::new)
                ),
                (buf, packet) -> buf
                        .writeInt(packet.getIdentificationValue())
                        .writeIP(packet.getTargetAddress())
                        .writeIPCollection(new ArrayList<>(packet.getAddresses())
                        ));

        register(DSRRouteReply.class,
                (buf, uuid, originatorIP, destinationIP) -> new DSRRouteReply(
                        uuid,
                        originatorIP,
                        destinationIP,
                        buf.readBoolean(),
                        buf.readIPCollection(ArrayList::new)
                ),
                (buf, packet) -> buf
                        .writeBoolean(packet.getLastHopExternalFlag())
                        .writeIPCollection(new ArrayList<>(packet.getAddresses())
                        ));

        register(DSRSourceRoute.class,
                (buf, uuid, originatorIP, destinationIP) -> new DSRSourceRoute(
                        uuid,
                        originatorIP,
                        destinationIP,
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readInt(),
                        buf.readIPCollection(ArrayList::new),
                        buf.readPacket()
                ),
                (buf, packet) -> buf
                        .writeBoolean(packet.getFirstHopExternalFlag())
                        .writeBoolean(packet.getLastHopExternalFlag())
                        .writeInt(packet.getSegLeft())
                        .writeIPCollection(packet.getAddresses())
                        .writePacket(packet.getPacket()),
                (packet, onClientSide) -> {
                    if (onClientSide) {
                        var innerPacket = packet.getPacket();
                        if (innerPacket instanceof PingPacket || innerPacket instanceof PingRepPacket) {
                            NetworkPacketCodecRegistry.handlerOf(innerPacket.getClass()).handle(innerPacket, onClientSide);
                        }
                    }
                });

        register(DSRRouteError.class,
                ((buf, uuid, originatorIP, destinationIP) -> new DSRRouteError(
                        uuid,
                        originatorIP,
                        destinationIP,
                        buf.readIP(),
                        buf.readIP(),
                        buf.readIP()
                )),
                (buf, packet) -> buf
                        .writeIP(packet.errorSourceAddress)
                        .writeIP(packet.errorDestinationAddress)
                        .writeIP(packet.unreachableNodeAddress)
        );

        register(LARRouteError.class,
                (buf, uuid, originatorIP, destinationIP) -> new LARRouteError(
                        uuid,
                        originatorIP,
                        destinationIP,
                        buf.readIP()
                ),
                (buf, packet) -> buf
                        .writeIP(packet.unreachableDestination)
        );

        register(LARRouteRequest.class,
                (buf, uuid, originatorIP, destinationIP) -> new LARRouteRequest(
                        uuid,
                        originatorIP,
                        destinationIP,
                        buf.readVec3(),
                        buf.readVec3(),
                        buf.readInt(),
                        buf.readInt()
                ),
                (buf, packet) -> buf
                        .writeVec3R(packet.sourcePos)
                        .writeVec3R(packet.destExpectedPos)
                        .writeInt(packet.requestZoneSize)
                        .writeInt(packet.hopCount)
        );

        register(LARRouteReply.class,
                (buf, uuid, originatorIP, destinationIP) -> new LARRouteReply(
                        uuid,
                        originatorIP,
                        destinationIP,
                        buf.readVec3(),
                        buf.readVec3()
                ),
                (buf, packet) -> buf
                        .writeVec3R(packet.sourcePos)
                        .writeVec3R(packet.destPos)
        );


        register(OLSRPacket.class, OLSRPacket.STREAM_CODEC);

    }


}
