package de.tyro.mcnetwork.block;

import de.tyro.mcnetwork.block.entity.NetworkNodeBlockEntity;
import de.tyro.mcnetwork.routing.core.SimulationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class NetworkNodeBlock extends Block {
    public NetworkNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            NetworkNodeBlockEntity be = (NetworkNodeBlockEntity) level.getBlockEntity(pos);

            SimulationRegistry.setSelectedNode(player, be.getNode());
        }

        return InteractionResult.SUCCESS;
    }
}
