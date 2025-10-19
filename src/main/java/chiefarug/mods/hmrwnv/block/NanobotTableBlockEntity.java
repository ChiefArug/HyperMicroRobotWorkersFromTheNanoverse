package chiefarug.mods.hmrwnv.block;

import chiefarug.mods.hmrwnv.HfmrnvRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class NanobotTableBlockEntity extends BlockEntity {
    public NanobotTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(HfmrnvRegistries.NANOBOT_TABLE_BE.get(), pos, blockState);
    }


}
