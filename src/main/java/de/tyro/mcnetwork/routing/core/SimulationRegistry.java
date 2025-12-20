package de.tyro.mcnetwork.routing.core;

import de.tyro.mcnetwork.routing.node.SimNode;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SimulationRegistry {

    private static final SimulationEngine ENGINE = SimulationEngine.INSTANCE;

    private static final Map<UUID, SimNode> selectedNodes = new HashMap<>();

    public static SimulationEngine getEngine() {
        return ENGINE;
    }

    public static void setSelectedNode(Player player, SimNode node) {
        selectedNodes.put(player.getUUID(), node);
    }

    public static SimNode getSelectedNode(Player player) {
        return selectedNodes.get(player.getUUID());
    }
}

