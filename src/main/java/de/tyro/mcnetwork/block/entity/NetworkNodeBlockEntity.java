package de.tyro.mcnetwork.block.entity;

import de.tyro.mcnetwork.routing.core.SimulationRegistry;
import de.tyro.mcnetwork.routing.node.SimNode;
import de.tyro.mcnetwork.routing.protocol.AodvProtocol;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class NetworkNodeBlockEntity extends BlockEntity {

    public SimNode getNode() {
        return node;
    }

    private final SimNode node;

    public NetworkNodeBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.NODE_BE.get(), pos, blockState);

        this.node = new SimNode(
                UUID.randomUUID(),
                null,
                pos,
                new AodvProtocol()
        );

        SimulationRegistry.getEngine().addNode(node);
    }


}
