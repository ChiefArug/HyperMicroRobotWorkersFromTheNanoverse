package chiefarug.mods.hmrwnv.block;

import chiefarug.mods.hmrwnv.HmrnvRegistries;
import chiefarug.mods.hmrwnv.core.NanobotSwarm;
import chiefarug.mods.hmrwnv.core.collections.EffectMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.LGGR;
import static net.minecraft.world.level.block.Block.UPDATE_ALL;

//TODO: make leds on texture change depending on effects
public class NanobotDiffuserBlockEntity extends BlockEntity {
    private static final int MAX_TICKS = 1000;

    public NanobotDiffuserBlockEntity(BlockPos pos, BlockState state) {
        super(HmrnvRegistries.NANOBOT_DIFFUSER_BE.get(), pos, state);
    }

    private int ticksRemaining = MAX_TICKS;
    @Nullable
    @Unmodifiable
    private EffectMap effects = null;

    void setEffects(EffectMap e) {
        effects = EffectMap.unmodifiableCopy(e);
        ticksRemaining = MAX_TICKS;
        setChanged();

    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);
        if (tag.contains("ticksRemaining"))
            ticksRemaining = tag.getInt("ticksRemaining");
        if (tag.contains("effects"))
            effects = EffectMap.CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("effects")).getOrThrow().getFirst();
        else
            effects = null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ticksRemaining", ticksRemaining);
        if (effects != null)
            tag.put("effects", EffectMap.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), effects).getOrThrow());
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, NanobotDiffuserBlockEntity::createUpdateTag);
    }

    private static CompoundTag createUpdateTag(BlockEntity be, RegistryAccess registryAccess) {
        CompoundTag tag = new CompoundTag();
        if (!(be instanceof NanobotDiffuserBlockEntity nbe)) return tag;
        if (nbe.effects != null) {
            DataResult<Tag> effData = EffectMap.CODEC.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), nbe.effects);
            effData.ifSuccess(t -> tag.put("e", t));
            effData.ifError(e -> LGGR.error("Failed to encode diffuser packet due to codec error: {}", e.message()));
        }
        tag.putInt("t", nbe.ticksRemaining);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        if (tag.contains("e")) {
            DataResult<Pair<EffectMap, Tag>> e = EffectMap.CODEC.decode(lookupProvider.createSerializationContext(NbtOps.INSTANCE), tag.get("e"));
            e.ifSuccess(m -> effects = m.getFirst());
            e.ifError(er -> LGGR.error("Failed to decode diffuser packet due to codec error: {}", er.message()));
        } else {
            effects = null;
        }
        if (tag.contains("t")) {
            ticksRemaining = tag.getInt("t");
        }
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        CompoundTag compoundtag = pkt.getTag();
        if (!compoundtag.isEmpty()) {
            // for some stupid reason this is not the default behaviour
            this.handleUpdateTag(compoundtag, lookupProvider);
        }
    }

    @Nullable
    @Unmodifiable
    public EffectMap getEffects() {
        return effects;
    }

    void tick(ServerLevel level, BlockPos pos, BlockState state) {
        if (effects == null) return;
        if (ticksRemaining-- <= 0) {
            NanobotSwarm.mergeSwarm(level.getChunk(pos), effects);
            effects = null;
            level.setBlock(pos, NanobotDiffuserBlock.damage(state, 5), UPDATE_ALL);
            level.playSound(null, getBlockPos(), HmrnvRegistries.NANOBOT_DIFFUSER.get().getSoundType(getBlockState(), level, pos, null).getBreakSound(), SoundSource.BLOCKS);
        }
        //TODO: override the sync packet so that the ticks remaining value syncs.
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public double percentTransformed() {
        return effects == null ? 0 : 1 - (double) ticksRemaining / MAX_TICKS;
    }
}
