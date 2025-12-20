package de.tyro.mcnetwork.routing.node;

import de.tyro.mcnetwork.routing.core.SimulationEngine;
import de.tyro.mcnetwork.routing.core.SimulationRegistry;
import de.tyro.mcnetwork.routing.packet.Packet;
import de.tyro.mcnetwork.routing.protocol.RoutingProtocol;
import net.minecraft.core.BlockPos;

import java.util.UUID;

public class SimNode {
    private final UUID id;
    private BlockPos pos;

    private final NodeData nodeData;
    private final RoutingProtocol protocol;

    public SimNode(UUID id, NodeData nodeData, BlockPos pos, RoutingProtocol protocol) {
        this.id = id;
        this.nodeData = nodeData;
        this.protocol = protocol;
    }

    public void receive(Packet packet) {
        protocol.onReceive(packet, this, SimulationEngine.INSTANCE.getNodeById(packet.getSource()));
    }

    public void tick() {
        protocol.onTick(this);
    }

    public UUID getId() {
        return id;
    }

    public BlockPos getPos() {
        return pos;
    }

    public NodeData getNodeData() {
        return nodeData;
    }

    public RoutingProtocol getProtocol() {
        return protocol;
    }
}
