package chiefarug.mods.hfmrwnv.block;

import chiefarug.mods.hfmrwnv.HfmrnvRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class NanobotTableBlockEntity extends BlockEntity {
    public NanobotTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(HfmrnvRegistries.NANOBOT_TABLE_BE.get(), pos, blockState);
    }


}
