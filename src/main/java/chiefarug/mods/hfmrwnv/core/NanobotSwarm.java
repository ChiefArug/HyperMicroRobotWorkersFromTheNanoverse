package chiefarug.mods.hfmrwnv.core;

import chiefarug.mods.hfmrwnv.core.effect.NanobotEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.AbstractObject2IntMap.BasicEntry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.util.strategy.IdentityStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static chiefarug.mods.hfmrwnv.HfmrnvRegistries.SWARM;


public final class NanobotSwarm {
    public static final Codec<NanobotSwarm> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            // "id": ResourceLocation
            ResourceLocation.CODEC.fieldOf("id").forGetter(NanobotSwarm::id),
            // "effects": [{
            RecordCodecBuilder.<Entry<NanobotEffect>>create(inst1 -> inst1.group(
                    //         "effect" NanobotEffect
                    NanobotEffect.CODEC.fieldOf("effect").forGetter(Entry::getKey),
                    //         "level" int
                    Codec.INT.fieldOf("level").forGetter(Entry::getIntValue)
            ).apply(inst1, BasicEntry::new)).listOf().fieldOf("effects").forGetter(NanobotSwarm::apply)
            // }]
    ).apply(inst, NanobotSwarm::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, NanobotSwarm> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, NanobotSwarm::id,
            ByteBufCodecs.map(NanobotSwarm::newMap, NanobotEffect.STREAM_CODEC, ByteBufCodecs.INT), NanobotSwarm::effects,
            NanobotSwarm::new
    );

    private final ResourceLocation id;
    private final Object2IntMap<NanobotEffect> effects;

    public ResourceLocation id() {
        return id;
    }

    private static Object2IntMap<NanobotEffect> effects(NanobotSwarm n) {
        return n.effects;
    }

    private NanobotSwarm(ResourceLocation id, Map<NanobotEffect, Integer> effects) {
        this.id = id;
        this.effects = new Object2IntOpenCustomHashMap<>(effects, IdentityStrategy.IDENTITY);
    }

    /// Attach a new swarm to this host. Replaces an existing swarm on that host, if present.
    public static NanobotSwarm attachSwarm(IAttachmentHolder host, ResourceLocation id, Map<NanobotEffect, Integer> effects) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            switch (host) {
                case Player safe_ignored -> {}
                case Entity safe_ignored -> {}
                case ChunkAccess safe_ignored -> {}
                default -> throw new IllegalArgumentException("Attaching swarm to unknown host type: " + host.getClass());
            }
        }

        NanobotSwarm swarm = host.getExistingDataOrNull(SWARM.attachment());
        // If there was already a swarm on the host then call its remove hooks as it shall be replaced imminently
        if (swarm != null) swarm.beforeRemove(host);

        swarm = new NanobotSwarm(id, effects);
        host.setData(SWARM.attachment(), swarm);
        swarm.afterAdd(host);
        return swarm;
    }

    /// Add or updates a single effect to this swarm with the specified level.
    public void addEffect(IAttachmentHolder host, NanobotEffect effect, int level) {
        effects.put(effect, level);
        effect.onAdd(host, level);
        markDirty(host);
    }

    /// Add multiple effects to this swarm at once. This is preferred over repeatedly calling {@link NanobotSwarm#addEffect} as it only syncs once.
    /// As a bonus it also batches operations, so all additions can see all the other additions in {@link NanobotEffect#onAdd}
    public void addEffects(IAttachmentHolder host, Object2IntMap<NanobotEffect> newEffects) {
        effects.putAll(newEffects);
        forEachEffect(newEffects, host, NanobotEffect::onAdd);
        markDirty(host);
    }

    /// Removes a single effect from this swarm. Does nothing if the effect is not part of this swarm.
    public void removeEffect(IAttachmentHolder host, NanobotEffect effect) {
        if (!effects.containsKey(effect))
            return;
        effect.onRemove(host, effects.getInt(effect));
        effects.removeInt(effect);
        markDirty(host);
    }

    /// Removes multiple effects from this swarm at once. This is preferred over repeatedly calling {@link NanobotSwarm#removeEffect} as it only syncs once.
    /// As a bonus it also batches operations, so all removals can see all the other removals in {@link NanobotEffect#onRemove}.
    public void removeEffects(IAttachmentHolder host, List<NanobotEffect> oldEffects) {
        for (NanobotEffect effect : oldEffects) {
            if (!effects.containsKey(effect)) continue;
            effect.onRemove(host, effects.getInt(effect));
        }
        for (NanobotEffect oldEffect : oldEffects) {
            effects.removeInt(oldEffect);
        }
        markDirty(host);
    }

    /// Called once when this swarm is first added to a host.
    public void afterAdd(IAttachmentHolder host) {
        forEachEffect(effects, host, NanobotEffect::onAdd);
    }

    /// Called once just before this swarm is removed from a host
    public void beforeRemove(IAttachmentHolder host) {
        forEachEffect(effects, host, NanobotEffect::onRemove);
    }

    /// Called frequently while this effect is part of a swarm is on something.
    /// The exact rate is configurable, but by default is every tick for players and entities, and evey second for chunks.
    public void tick(IAttachmentHolder host) {
        forEachEffect(effects, host, NanobotEffect::onTick);
    }

    private interface EffectConsumer {
        void accept(NanobotEffect effect, IAttachmentHolder host, int level);
    }

    private static void forEachEffect(Object2IntMap<NanobotEffect> effects, IAttachmentHolder host, EffectConsumer consumer) {
        for (Entry<NanobotEffect> entry : effects.object2IntEntrySet()) {
            consumer.accept(entry.getKey(), host, entry.getIntValue());
        }
    }

    ///  Mark this as dirty when modified so it can be marked as needing to save and resynced to clients
    void markDirty(IAttachmentHolder host) {
        host.setData(SWARM.attachment(), this);
    }

    private static @NotNull Object2IntOpenCustomHashMap<NanobotEffect> newMap(int i) {
        return new Object2IntOpenCustomHashMap<>(i, IdentityStrategy.IDENTITY);
    }

    private NanobotSwarm(ResourceLocation id, List<Entry<NanobotEffect>> effects) {
        this(id, newMap(effects.size()));
        for (var effect : effects) this.effects.put(effect.getKey(), effect.getIntValue());
    }

    private List<Entry<NanobotEffect>> apply(NanobotSwarm this) {
        return new ArrayList<>(this.effects.object2IntEntrySet());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NanobotSwarm that)) return false;

        return id.equals(that.id) && effects.equals(that.effects);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + effects.hashCode();
        return result;
    }
}
