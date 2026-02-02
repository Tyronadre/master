package de.tyro.mcnetwork.routing.core;

import de.tyro.mcnetwork.routing.node.SimNode;

import java.util.Collection;
import java.util.List;

public class TopologyManager {

    private final double radioRange;

    public TopologyManager(double radioRange) {
        this.radioRange = radioRange;
    }

    public List<SimNode> getNeighbors(SimNode node, Collection<SimNode> allNodes) {
        return allNodes.stream()
                .filter(other -> !other.equals(node))
                .filter(other ->
                        other.getPos().distSqr(node.getPos()) <= radioRange * radioRange
                )
                .toList();
    }
}
 