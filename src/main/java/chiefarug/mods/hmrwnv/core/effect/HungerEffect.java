package chiefarug.mods.hmrwnv.core.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
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
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.LGGR;
import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODRL;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static net.minecraft.world.level.block.ComposterBlock.LEVEL;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

/// An effect that consumes something from the target to provide energy.
/// Entities do not have anything to consume so this provides free energy to them.
public class HungerEffect implements NanobotEffect.NonStateful {
    private static final Codec<BlockState> TRANSFORM_RESULT_CODEC = Codec.withAlternative(BlockState.CODEC, BuiltInRegistries.BLOCK.byNameCodec().xmap(Block::defaultBlockState, BlockState::getBlock));
    public static final MapCodec<HungerEffect> CODEC = RecordCodecBuilder.mapCodec(g -> g.group(
            Codec.INT.fieldOf("decay_rate").forGetter(HungerEffect::decayRate),
            Codec.DOUBLE.fieldOf("player_exhaustion").forGetter(HungerEffect::playerExhaustion),
            LEVEL_MULTIPLIER.forGetter(HungerEffect::level)
    ).apply(g, HungerEffect::new));

    private static final DataMapType<Block, BlockState> HUNGER_TRANSFORM = DataMapType.builder(MODRL.withPath("hunger_transform"), Registries.BLOCK, TRANSFORM_RESULT_CODEC).build();
    private static final Map<Block, Function<BlockState, @Nullable BlockState>> STATE_AWARE_TRANSFORM = Collections.synchronizedMap(new HashMap<>());

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

    private static final int COMPOSTER_MAX_LEVEL = 8;
    private final int decayRate;
    private final double playerExhaustion;
    private final int level;

    public HungerEffect(int decayRate, double playerExhaustion, int level) {
        this.decayRate = decayRate;
        this.playerExhaustion = playerExhaustion;
        this.level = level;
    }

    static {
        registerTransformation(Blocks.COMPOSTER, state -> {
            if (state.getValue(LEVEL) == COMPOSTER_MAX_LEVEL)
                // subtract two if its the max level, otherwise nothing changes
                return state.setValue(LEVEL, COMPOSTER_MAX_LEVEL - 2);
            return state.setValue(LEVEL, Math.max(state.getValue(LEVEL) - 1, 0));
        });
    }

    @Override
    public MapCodec<? extends NanobotEffect> codec() {
        return CODEC;
    }

    @Override
    public void onTick(IAttachmentHolder host, int effectLevel) {
        switch (host) {
            case Player player -> player.causeFoodExhaustion(getExhaustion(effectLevel));
            case LevelChunk chunk -> {
                LevelChunkSection[] chunkSections = chunk.getSections();

                for (int i = 0; i < chunkSections.length; i++) {
                    LevelChunkSection section = chunkSections[i];
                    if (section.hasOnlyAir()) continue;
                    ServerLevel level = (ServerLevel) chunk.getLevel();
                    int minY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
                    int minX = chunk.getPos().getMinBlockX();
                    int minZ = chunk.getPos().getMinBlockZ();

                    int blocks = decayRate * effectLevel;
                    for (int j = 0; j < blocks; j++) {
                        BlockPos pos = level.getBlockRandomPos(minX, minY, minZ, 15);
                        BlockState state = section.getBlockState(pos.getX() - minX, pos.getY() - minY, pos.getZ() - minZ);
                        if (canTransform(state, pos, level)) {
                            BlockState newState = transform(state);
                            if (state != newState) {
                                // manually set it and queue a resync to the client to avoid block updates
                                section.setBlockState(pos.getX() - minX, pos.getY() - minY, pos.getZ() - minZ, newState);
                                ChunkHolder chunkHolder = level.getChunkSource().chunkMap.getVisibleChunkIfPresent(chunk.getPos().toLong());
                                if (chunkHolder != null)
                                    chunkHolder.blockChanged(pos);
                            }
                        }
                    }
                }
            }
            default -> {}
        }
    }

    protected float getExhaustion(int effectLevel) {
        return (float) (playerExhaustion * effectLevel);
    }

    @Override
    public int getRequiredPower(int level) {
        return this.level * level;
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterDataMapTypesEvent event) -> event.register(HungerEffect.HUNGER_TRANSFORM));
    }

    public int decayRate() {return decayRate;}

    public double playerExhaustion() {return playerExhaustion;}

    public int level() {return level;}

    @Override
    public String toString() {
        return "HungerEffect[" +
                "decayRate=" + decayRate + ", " +
                "playerExhaustion=" + playerExhaustion + ", " +
                "level=" + level + ']';
    }
}
