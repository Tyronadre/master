package de.tyro.mcnetwork.routing.packet;

import de.tyro.mcnetwork.routing.INetworkNode;
import de.tyro.mcnetwork.routing.IP;

import java.util.List;

public class AODVRREQPacket extends NetworkPacket implements IProtocolPaket {
    public final boolean joinFlag;
    public final boolean repairFlag;
    public final boolean gratuitousFlag;
    public final boolean destinationOnlyFlag;
    public final boolean unknownSeqFlag;

    public int hopCount;
    public final int rreqId;
    public final int destinationSequenceNumber;
    public final int originatorSequenceNumber;

    protected AODVRREQPacket(IP sourceIp, IP destinationIp, IP prev,  boolean joinFlag, boolean repairFlag, boolean gratuitousFlag, boolean destinationOnlyFlag, boolean unknownSeqFlag, int hopCount, int rreqId, int destinationSequenceNumber, int originatorSequenceNumber) {
        super(sourceIp, destinationIp, prev);
        this.joinFlag = joinFlag;
        this.repairFlag = repairFlag;
        this.gratuitousFlag = gratuitousFlag;
        this.destinationOnlyFlag = destinationOnlyFlag;
        this.unknownSeqFlag = unknownSeqFlag;
        this.hopCount = hopCount;
        this.rreqId = rreqId;
        this.destinationSequenceNumber = destinationSequenceNumber;
        this.originatorSequenceNumber = originatorSequenceNumber;
    }

    public AODVRREQPacket(IP sourceIp, IP destinationIp, IP prev, boolean unknownSeqFlag, int destinationSequenceNumber, int originatorSequenceNumber, int rreqId, int hopCount) {
        this(sourceIp, destinationIp, prev, false, false, false, false, unknownSeqFlag, hopCount, rreqId, destinationSequenceNumber, originatorSequenceNumber);
    }

    @Override
    public List<String> getRenderContent() {
        return List.of(
                "Origin: " + sourceIp + " @ " + originatorSequenceNumber,
                "Destination " + destinationIp + " @ " + destinationSequenceNumber
        );
    }

    public NetworkPacket hop(INetworkNode self) {
        return new AODVRREQPacket(sourceIp, destinationIp, self.getIP(), joinFlag, repairFlag, gratuitousFlag, destinationOnlyFlag, unknownSeqFlag, hopCount++, rreqId, destinationSequenceNumber, originatorSequenceNumber);
    }
}
