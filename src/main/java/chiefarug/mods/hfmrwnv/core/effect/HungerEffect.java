package chiefarug.mods.hfmrwnv.core.effect;

import chiefarug.mods.hfmrwnv.HfmrnvConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import static chiefarug.mods.hfmrwnv.HyperFungusMicroRobotWorkersFromTheNanoverse.LGGR;
import static chiefarug.mods.hfmrwnv.HyperFungusMicroRobotWorkersFromTheNanoverse.MODRL;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static net.minecraft.world.level.block.ComposterBlock.LEVEL;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

/// An effect that consumes something from the target to provide energy.
/// Entities do not have anything to consume so this provides free energy to them.
public class HungerEffect implements NanobotEffect.NonStateful, NanobotEffect.Unit {
    private static final Codec<BlockState> TRANSFORM_RESULT_CODEC = Codec.withAlternative(BlockState.CODEC, BuiltInRegistries.BLOCK.byNameCodec().xmap(Block::defaultBlockState, BlockState::getBlock));
    private static final DataMapType<Block, BlockState> HUNGER_TRANSFORM = DataMapType.builder(MODRL.withPath("hunger_transform"), Registries.BLOCK, TRANSFORM_RESULT_CODEC).build();
    private static final Map<Block, UnaryOperator<@Nullable BlockState>> STATE_AWARE_TRANSFORM = Collections.synchronizedMap(new HashMap<>());

    /// Get the transformed version of this block.
    /// Prioritises the datamap to allow datamaps to override mod's in-code decisions.
    /// Data supremacy!
    @NotNull
    public BlockState transform(final BlockState state) {
        BlockState outState = state.getBlockHolder().getData(HUNGER_TRANSFORM);

        if (outState == null && STATE_AWARE_TRANSFORM.containsKey(state.getBlock())) {
            outState = STATE_AWARE_TRANSFORM.get(state.getBlock()).apply(state);
            return outState == null ? state : outState;
        } else if (outState != null) {
            // best-attempt copy fluids if fetched from datamap
            if (state.getFluidState().getFluidType() != outState.getFluidState().getFluidType()) {
                if (state.getFluidState().is(Fluids.WATER) && outState.hasProperty(WATERLOGGED))
                    outState = outState.setValue(WATERLOGGED, true);
                else if (outState.isAir())
                    outState = state.getFluidState().createLegacyBlock();
            }
            // copy all properties
            for (Property<?> property : state.getProperties())
                if (outState.hasProperty(property))
                    outState = copyProperty(property, state, outState);
        }

        return outState == null ? state : outState;
    }

    protected boolean canTransform(BlockState state, BlockPos pos, Level level) {
        return !state.isAir();
    }

    private static <T extends Comparable<T>> BlockState copyProperty(Property<T> prop, BlockState source, BlockState dest) {
        return dest.setValue(prop, source.getValue(prop));
    }

    /// Register a state aware blockstate transformer for when the hunger effect is consuming a chunk.
    /// If you do not need state context please use the hunger_transform datamap instead!
    public static void registerTransformation(Block block, UnaryOperator<BlockState> stateMapper) {
        if (STATE_AWARE_TRANSFORM.containsKey(block)) {
            LGGR.warn("Class {} overrode a state aware transform for block {}", StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass(), BuiltInRegistries.BLOCK.getKey(block));
        }
        STATE_AWARE_TRANSFORM.put(block, stateMapper);
    }
    static {
        registerTransformation(Blocks.COMPOSTER, state -> state.setValue(LEVEL, Math.max(state.getValue(LEVEL) - 1, 0)));
    }

    @Override
    public void onTick(IAttachmentHolder host, int effectLevel) {
        switch(host) {
            case Player player -> player.causeFoodExhaustion((float) (HfmrnvConfig.HUNGER_EXHAUSTION.getAsDouble() * effectLevel));
            case LevelChunk chunk -> {
                LevelChunkSection[] alevelchunksection = chunk.getSections();

                for (int i = 0; i < alevelchunksection.length; i++) {
                    LevelChunkSection levelchunksection = alevelchunksection[i];
                    if (levelchunksection.hasOnlyAir()) continue;
                    Level level = chunk.getLevel();
                    int minY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
                    int minX = chunk.getPos().getMinBlockX();
                    int minZ = chunk.getPos().getMinBlockZ();

                    int blocks = HfmrnvConfig.HUNGER_CHUNK_DECAY.getAsInt() * effectLevel;
                    for (int j = 0; j < blocks; j++) {
                        BlockPos pos = level.getBlockRandomPos(minX, minY, minZ, 15);
                        BlockState state = levelchunksection.getBlockState(pos.getX() - minX, pos.getY() - minY, pos.getZ() - minZ);
                        if (canTransform(state, pos, level)) {
                            BlockState newState = transform(state);
                            if (state != newState)
                                level.setBlock(pos, newState, Block.UPDATE_ALL);
                        }
                    }
                }
            }
            default -> {}
        }
    }

    @Override
    public int getRequiredPower(int level) {
        return -2 * level;
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterDataMapTypesEvent event) -> event.register(HungerEffect.HUNGER_TRANSFORM));
    }
}
