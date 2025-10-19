package chiefarug.mods.hmrwnv.core.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/// A safer version of {@link RavenousEffect} that does not touch block entities.
public class SafeRavenousEffect extends RavenousEffect {
    @Override
    protected boolean canTransform(BlockState state, BlockPos inPos, Level level) {
        return !(level.getBlockEntity(inPos) == null) && super.canTransform(state, inPos, level);
    }
}
