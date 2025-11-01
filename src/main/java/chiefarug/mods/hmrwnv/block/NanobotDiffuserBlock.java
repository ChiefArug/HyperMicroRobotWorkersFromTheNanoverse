package chiefarug.mods.hmrwnv.block;

import chiefarug.mods.hmrwnv.HmrnvRegistries;
import chiefarug.mods.hmrwnv.core.collections.EffectMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.SWARM;

public class NanobotDiffuserBlock extends Block implements EntityBlock {
    public static final MapCodec<NanobotDiffuserBlock> CODEC = simpleCodec(NanobotDiffuserBlock::new);
    public static final int MAX_DAMAGE = 15; //TODO: add another property that says if there is an item in here so we arent ticking empty diffusers
    public static final IntegerProperty DAMAGE = IntegerProperty.create("damage", 0, MAX_DAMAGE);

    public NanobotDiffuserBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DAMAGE);
        super.createBlockStateDefinition(builder);
    }


    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() == HmrnvRegistries.NANOBOTS.asItem())  {
            EffectMap effects = stack.get(SWARM);
            if (effects != null && level.getBlockEntity(pos) instanceof NanobotDiffuserBlockEntity blockEntity) {
                blockEntity.setEffects(effects);
                stack.shrink(1);
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected MapCodec<NanobotDiffuserBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NanobotDiffuserBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked") // its safe due to != check
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> bet) {
        if (bet != HmrnvRegistries.NANOBOT_DIFFUSER_BE.get()) return null;
        if (l instanceof ServerLevel) {
            BlockEntityTicker<NanobotDiffuserBlockEntity> tBlockEntityTicker = (level, pos, state, blockEntity) -> blockEntity.tick((ServerLevel) level, pos, state);
            return (BlockEntityTicker<T>) tBlockEntityTicker;
        }
        return null;
    }
}
