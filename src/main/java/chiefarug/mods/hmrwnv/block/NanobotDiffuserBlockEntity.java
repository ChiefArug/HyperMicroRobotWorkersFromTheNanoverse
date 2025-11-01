package chiefarug.mods.hmrwnv.block;

import chiefarug.mods.hmrwnv.HmrnvRegistries;
import chiefarug.mods.hmrwnv.core.NanobotSwarm;
import chiefarug.mods.hmrwnv.core.collections.EffectArrayMap;
import chiefarug.mods.hmrwnv.core.collections.EffectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

//TODO: make leds on texture change depending on effects
public class NanobotDiffuserBlockEntity extends BlockEntity {
    private static final int MAX_TICKS = 1000;

    public NanobotDiffuserBlockEntity(BlockPos pos, BlockState state) {
        super(HmrnvRegistries.NANOBOT_DIFFUSER_BE.get(), pos, state);
    }

    private int ticksRemaining = MAX_TICKS;
    @Nullable
    private EffectMap effects = null;

    void setEffects(EffectMap e) {
        effects = new EffectArrayMap(e);
        ticksRemaining = MAX_TICKS;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);
        ticksRemaining = tag.getInt("ticksRemaining");
        effects = EffectMap.CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("effects")).getOrThrow().getFirst();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ticksRemaining", ticksRemaining);
        if (effects != null)
            tag.put("effects", EffectMap.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), effects).getOrThrow());
    }

    void tick(ServerLevel level, BlockPos pos, BlockState state) {
        if (effects == null) return;
        if (ticksRemaining-- <= 0) {
            NanobotSwarm.mergeSwarm(level.getChunk(pos), effects);
            effects = null;
        }
        setChanged();
    }
}
