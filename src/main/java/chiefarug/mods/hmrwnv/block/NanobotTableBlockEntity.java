package chiefarug.mods.hmrwnv.block;

import chiefarug.mods.hmrwnv.HmrnvRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class NanobotTableBlockEntity extends BlockEntity {
    public NanobotTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(HmrnvRegistries.NANOBOT_TABLE_BE.get(), pos, blockState);
    }


}
