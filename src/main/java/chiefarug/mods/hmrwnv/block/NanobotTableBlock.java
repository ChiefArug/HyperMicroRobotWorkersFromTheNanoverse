package chiefarug.mods.hmrwnv.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class NanobotTableBlock extends BaseEntityBlock  {
    public static final MapCodec<NanobotTableBlock> CODEC = simpleCodec(NanobotTableBlock::new);


    public NanobotTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NanobotTableBlockEntity(pos, state);
    }
}
