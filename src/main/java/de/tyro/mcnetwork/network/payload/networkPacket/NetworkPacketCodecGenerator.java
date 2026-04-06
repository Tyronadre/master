package de.tyro.mcnetwork.network.payload.networkPacket;


import de.tyro.mcnetwork.network.BetterByteBuf;
import de.tyro.mcnetwork.routing.IP;
import de.tyro.mcnetwork.routing.SimulationEngine;
import de.tyro.mcnetwork.routing.packet.aodv.AODVRERRPacket;
import de.tyro.mcnetwork.routing.packet.aodv.AODVRREPPacket;
import de.tyro.mcnetwork.routing.packet.aodv.AODVRREQPacket;
import de.tyro.mcnetwork.routing.packet.application.DestinationUnreachablePacket;
import de.tyro.mcnetwork.routing.packet.application.PingPacket;
import de.tyro.mcnetwork.routing.packet.application.PingRepPacket;
import de.tyro.mcnetwork.routing.packet.application.TraceRoutePacket;
import de.tyro.mcnetwork.routing.packet.application.TraceRouteReplyPacket;
import de.tyro.mcnetwork.routing.packet.dsr.DSRRouteReply;
import de.tyro.mcnetwork.routing.packet.dsr.DSRRouteRequest;
import de.tyro.mcnetwork.routing.packet.dsr.DSRSourceRoute;
import de.tyro.mcnetwork.routing.packet.lar.LARRouteError;
import de.tyro.mcnetwork.routing.packet.lar.LARRouteReply;
import de.tyro.mcnetwork.routing.packet.lar.LARRouteRequest;
import de.tyro.mcnetwork.routing.packet.olsr.HelloPacket;
import de.tyro.mcnetwork.routing.packet.olsr.TCPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.core.pattern.FormattingInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import static de.tyro.mcnetwork.network.payload.networkPacket.NetworkPacketCodecRegistry.register;

public class NetworkPacketCodecGenerator {
    public static void generate() {
        register(DestinationUnreachablePacket.class,
                (buf, uuid, originatorIP, destinationIP) -> new DestinationUnreachablePacket(originatorIP, destinationIP),
                (buf, packet) -> {
                }
        );

        register(PingPacket.class,
                (buf, uuid, originatorIP, destinationIP) -> new PingPacket(uuid, originatorIP, destinationIP, 0),
                (buf, packet) -> {
                },
                (packet, onClientSide) -> packet.sendStartTime = SimulationEngine.getInstance(onClientSide).getSimTime()
        );

        register(PingRepPacket.class,
                (buf, uuid, ip1, ip2) -> new PingRepPacket(
                        uuid,
                        ip1,
                        ip2,
                        buf.readInt(),
                        0,
                        buf.readUUID()),
                (buf, packet) -> buf
                        .writeInt(packet.sendTime)
                        .writeUUID(packet.replyUUID),
                (packet, onClientSide) -> packet.returnStartTime = SimulationEngine.getInstance(onClientSide).getSimTime()
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

        register(HelloPacket.class,
                (buf, uuid, originatorIP, destinationIP) -> new HelloPacket(
                        uuid,
                        originatorIP,
                        new HashSet<>(buf.readIPList()),
                        buf.readInt()
                ),
                (buf, packet) -> buf
                        .writeIPList(new ArrayList<>(packet.neighbors))
                        .writeInt(packet.willingness)
        );

        register(TCPacket.class,
                (buf, uuid, originatorIP, destinationIP) -> new TCPacket(
                        uuid,
                        originatorIP,
                        buf.readMap(
                                buffer -> BetterByteBuf.IP_STREAM_CODEC.decode(buf),
                                (StreamDecoder<FriendlyByteBuf, Set<IP>>) buffer -> new HashSet<>(buffer.readCollection(HashSet::new, buffer1 -> BetterByteBuf.IP_STREAM_CODEC.decode(buffer1)))
                        )
                ),
                (buf, packet) ->
                        buf.writeMap(packet.advertisedLinks,
                                (buffer, value) -> BetterByteBuf.IP_STREAM_CODEC.encode(buffer, value),
                                (buffer, value) -> buffer.writeCollection(value, (buffer1, value1) -> BetterByteBuf.IP_STREAM_CODEC.encode(buffer1, value1))
                        )
        );

    }


}
