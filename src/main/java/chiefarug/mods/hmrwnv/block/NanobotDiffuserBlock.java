package chiefarug.mods.hmrwnv.block;

import chiefarug.mods.hmrwnv.HmrnvRegistries;
import chiefarug.mods.hmrwnv.core.EffectConfiguration;
import chiefarug.mods.hmrwnv.core.NanobotSwarm;
import chiefarug.mods.hmrwnv.core.collections.EffectMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.SWARM;
import static net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT;

public class NanobotDiffuserBlock extends Block implements EntityBlock {
    public static final MapCodec<NanobotDiffuserBlock> CODEC = simpleCodec(NanobotDiffuserBlock::new);
    public static final int MAX_DAMAGE = 14; //TODO: add another property that says if there is an item in here so we arent ticking empty diffusers
    public static final IntegerProperty DAMAGE = IntegerProperty.create("damage", 0, MAX_DAMAGE);

    public NanobotDiffuserBlock(Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DAMAGE);
        super.createBlockStateDefinition(builder);
    }


    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() == HmrnvRegistries.NANOBOTS.asItem()) {
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

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof NanobotDiffuserBlockEntity nbe)) return;
        EffectMap effects = nbe.getEffects();
        if (effects == null || effects.isEmpty()) return;

        int baseX = pos.getX();
        int baseY = pos.getY();
        int baseZ = pos.getZ();

        int n = 1 + (int) (30 * nbe.percentTransformed());
        for (int i = 0; i < n; i++) {
            int color = randomColor(effects, random);
            ColorParticleOption options = ColorParticleOption.create(ENTITY_EFFECT, color);


            double x = randomPosOutsideBlock(baseX, random);
            double y = randomPosOutsideBlock(baseY, random);
            double z = randomPosOutsideBlock(baseZ, random);
            double dx = Mth.nextDouble(random, 0, 0.1);
            double dy = Mth.nextDouble(random, 0, 0.1);
            double dz = Mth.nextDouble(random, 0, 0.1);
            level.addParticle(options, x, y, z, dx, dy, dz);
        }
    }

    private int randomColor(EffectMap effects, RandomSource random) {
        Holder<EffectConfiguration<?>> effect = NanobotSwarm.randomEffect(random, effects);
        //noinspection DataFlowIssue // not an issue cause it only returns null if effects is empty.
        EffectConfiguration<?> value = effect.value();
        return value.colorWithTransparency();
    }

    private static double randomPosOutsideBlock(int base, RandomSource random) {
        // split between two directions
        double p = random.nextDouble() - 0.5;
        // shift in the direction chosen
        if (p < 0) p -= 0.5;
        else p += 0.5;
        // final 0.5 centers it
        return base + p + 0.5;
    }

    public static BlockState damage(BlockState state, int damage) {
        int newValue = state.getValue(DAMAGE) + damage;
        // this gets destroyed even by the regular hunger effect
        if (newValue > MAX_DAMAGE) return Blocks.AIR.defaultBlockState();
        return state.setValue(DAMAGE, newValue);
    }

}
