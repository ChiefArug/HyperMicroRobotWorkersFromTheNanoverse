package chiefarug.mods.hmrwnv.core.effect;

import chiefarug.mods.hmrwnv.HmrnvRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.SAFE_RAVENOUS_WHITELIST;

public class RavenousEffect extends HungerEffect {
    public static final MapCodec<RavenousEffect> CODEC = RecordCodecBuilder.mapCodec(g -> g.group(
            Codec.INT.fieldOf("decay_rate").forGetter(HungerEffect::decayRate),
            Codec.DOUBLE.fieldOf("player_exhaustion").forGetter(HungerEffect::playerExhaustion),
            Codec.BOOL.fieldOf("keep_block_entities").forGetter(RavenousEffect::keepBlockEntities)
    ).apply(g, RavenousEffect::new));

    private final boolean keepBlockEntities;

    public RavenousEffect(int decayRate, double playerExhaustion, boolean keepBlockEntities) {
        super(decayRate, playerExhaustion);
        this.keepBlockEntities = keepBlockEntities;
    }

    public boolean keepBlockEntities() {
        return keepBlockEntities;
    }


    @Override
    protected boolean canTransform(BlockState state, BlockPos inPos, Level level) {
        if (
                !super.canTransform(state, inPos, level) ||
                state.is(HmrnvRegistries.RAVENOUS_BLACKLIST) ||
                keepBlockEntities && level.getBlockEntity(inPos) != null && !state.is(SAFE_RAVENOUS_WHITELIST)
        ) return false;

        int airCount = 0;
        BlockPos.MutableBlockPos pos = inPos.mutable();
        Direction[] allDirs = Direction.values();
        Direction[] airDirs = new Direction[6];
        for (Direction dir : allDirs) {
            pos.move(dir, 1);
            if (level.getBlockState(pos).isAir()) {
                airDirs[airCount++] = dir;
            }
            pos.move(dir, -1);
        }

        if (airCount >= 3) return true;

        // look in 3 manhattan distance
        final int directFound = airCount;
        for (int i = 0; i < directFound; i++) {
            Direction dir1 = airDirs[i];
            pos.move(dir1, 1);
            for (Direction dir2 : allDirs) {
                if (dir2 == dir1.getOpposite()) continue;
                pos.move(dir2, 1);
                for (Direction dir3 : allDirs) {
                    if (dir3.getOpposite() == dir1 || dir3.getOpposite() == dir2) continue;
                    pos.move(dir3, 1);
                    if (level.getBlockState(pos).isAir())
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

    @Override
    public String toString() {
        return "RavenousEffect[" +
                "keepBlockEntities=" + keepBlockEntities + ", " +
                "decayRate=" + decayRate() + ", " +
                "playerExhaustion=" + playerExhaustion() + ']';
    }
}
