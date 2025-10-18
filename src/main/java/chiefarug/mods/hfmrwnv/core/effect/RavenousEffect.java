package chiefarug.mods.hfmrwnv.core.effect;

import chiefarug.mods.hfmrwnv.HfmrnvRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.core.Direction.*;

public class RavenousEffect extends HungerEffect {

    private static final Direction[] DIRS = new Direction[]{DOWN, UP, NORTH, SOUTH, WEST, EAST};

    @Override
    protected boolean canTransform(BlockState state, BlockPos inPos, Level level) {
        if (!super.canTransform(state, inPos, level) || state.is(HfmrnvRegistries.RAVENOUS_BLACKLIST) || !state.getFluidState().isEmpty()) return false;

        int airCount = 0;
        BlockPos.MutableBlockPos pos = inPos.mutable();
        Direction[] dirs = new Direction[6];
        for (Direction dir : DIRS) {
            pos.move(dir, 1);
            if (level.getBlockState(pos).canBeReplaced()) {
                dirs[airCount++] = dir;
            }
            pos.move(dir, -1);
        }

        if (airCount >= 3) return true;

        // look in 3 manhattan distance
        final int directFound = airCount;
        for (int i = 0; i < directFound; i++) {
            Direction dir1 = dirs[i];
            pos.move(dir1, 1);
            for (Direction dir2 : DIRS) {
                if (dir2 == dir1.getOpposite()) continue;
                pos.move(dir2, 1);
                for (Direction dir3 : DIRS) {
                    if (dir3.getOpposite() == dir1 || dir3.getOpposite() == dir2) continue;
                    pos.move(dir3, 1);
                    if (level.getBlockState(pos).canBeReplaced())
                        if (airCount++ >= 20)
                            return true;
                    pos.move(dir3, -1);
                }
                pos.move(dir2, -1);
            }
            pos.move(dir1, -1);
        }
        return false;
    }

    @Override
    public @NotNull BlockState transform(BlockState state) {
        BlockState newState = super.transform(state);
        if (newState == state)
           return Blocks.AIR.defaultBlockState();
        return newState;
    }
}
