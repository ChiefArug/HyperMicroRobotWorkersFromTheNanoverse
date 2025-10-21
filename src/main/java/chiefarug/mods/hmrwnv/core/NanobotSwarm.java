package chiefarug.mods.hmrwnv.core;

import chiefarug.mods.hmrwnv.core.effect.NanobotEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.AbstractObject2IntMap.BasicEntry;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.util.strategy.IdentityStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.SWARM;


public final class NanobotSwarm {
    public static final Codec<NanobotSwarm> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            // "effects": [{
            RecordCodecBuilder.<Entry<NanobotEffect>>create(inst1 -> inst1.group(
                    //     "effect" NanobotEffect
                    NanobotEffect.CODEC.fieldOf("effect").forGetter(Entry::getKey),
                    //     "level" int
                    Codec.INT.fieldOf("level").forGetter(Entry::getIntValue)
            ).apply(inst1, BasicEntry::new)).listOf().fieldOf("effects").forGetter(NanobotSwarm::apply)
            // }]
    ).apply(inst, NanobotSwarm::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, NanobotSwarm> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(NanobotSwarm::newMap, NanobotEffect.STREAM_CODEC, ByteBufCodecs.INT), n -> n.effects,
            NanobotSwarm::new
    );

    private final Object2IntMap<NanobotEffect> effects;

    private NanobotSwarm(Map<NanobotEffect, Integer> effects) {
        this.effects = new Object2IntOpenCustomHashMap<>(effects, IdentityStrategy.IDENTITY);
    }

    /// Attach a new swarm to this host. Replaces an existing swarm on that host, if present.
    public static NanobotSwarm attachSwarm(IAttachmentHolder host, Map<NanobotEffect, Integer> effects) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            switch (host) {
                case Player ignored -> {}
                case Entity ignored -> {}
                case ChunkAccess ignored -> {}
                default -> throw new IllegalArgumentException("Attaching swarm to unknown host type: " + host.getClass());
            }
        }

        NanobotSwarm swarm = host.getExistingDataOrNull(SWARM);
        // If there was already a swarm on the host then call its remove hooks as it shall be replaced imminently
        if (swarm != null) swarm.beforeRemove(host);

        swarm = new NanobotSwarm(effects);
        host.setData(SWARM, swarm);
        swarm.afterAdd(host);
        return swarm;
    }

    /// Merge these effects into the host, swarming them if they are not already swarmed otherwise adding any effects that are a higher level
    public static void mergeSwarm(IAttachmentHolder host, Object2IntMap<NanobotEffect> inEffects) {
        Optional<NanobotSwarm> existing = host.getExistingData(SWARM);
        if (existing.isPresent()) {
            NanobotSwarm existingSwarm = existing.get();

            // these are used to build an array map
            Object[] newKeys = new Object[inEffects.size()];
            int[] newValues = new int[inEffects.size()];
            int i = 0;

            // copy over the entries that are higher level
            for (Entry<NanobotEffect> entry : Object2IntMaps.fastIterable(inEffects)) {
                NanobotEffect effect = entry.getKey();
                int level = entry.getIntValue();
                int existingLevel = existingSwarm.getEffectLevel(effect).orElse(0);

                // if the existing level is less the new level, add it to the arrays to merge in
                // and if its not 0, run its remove trigger
                if (existingLevel < level) {
                    if (existingLevel != 0)
                        effect.onRemove(host, level);
                    newKeys[i] = effect;
                    newValues[i++] = level;
                }
            }

            Object2IntArrayMap<NanobotEffect> newEffects = new Object2IntArrayMap<>(newKeys, newValues, i);
            existingSwarm.effects.putAll(newEffects);

            forEachEffect(newEffects, host, NanobotEffect::onAdd);
            existingSwarm.markDirty(host);
        } else {
            attachSwarm(host, inEffects);
        }
    }

    /// Clears any swarm on the host
    public static void clearSwarm(IAttachmentHolder host) {
        Optional<NanobotSwarm> mayExist = host.getExistingData(SWARM);
        if (mayExist.isEmpty()) return;

        forEachEffect(mayExist.get().effects, host, NanobotEffect::onRemove);

        host.removeData(SWARM);
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
    public void removeEffects(IAttachmentHolder host, Collection<NanobotEffect> oldEffects) {
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
    /// The exact rate is configurable, but by default is every tick for players and entities, and twice a second for chunks.
    public void tick(IAttachmentHolder host) {
        forEachEffect(effects, host, NanobotEffect::onTick);
    }

    public boolean hasEffect(Supplier<? extends NanobotEffect> effect) {
        return hasEffect(effect.get());
    }

    public boolean hasEffect(NanobotEffect effect) {
        return effects.containsKey(effect);
    }

    public OptionalInt getEffectLevel(NanobotEffect effect) {
        return effects.containsKey(effect) ? OptionalInt.of(effects.getInt(effect)) : OptionalInt.empty();
    }

    public boolean isActive() {
        return getPowerTotal() >= 0;
    }

    private int getPowerTotal() {
        return effects.object2IntEntrySet().stream()
                .mapToInt(e -> e.getKey().getRequiredPower(e.getIntValue()))
                .sum();
    }

    /// Get a random effect weighted by effect levels
    @NotNull
    public NanobotEffect randomEffect(RandomSource random) {
        if (effects.isEmpty()) throw new IllegalStateException("NanobotSwarm should always have effects!");

        // not null cause we assert its not empty, and it only returns null on empty maps.
        //noinspection DataFlowIssue
        return randomEffect(random, effects);
    }

    @Nullable
    public NanobotEffect randomEffectExcept(RandomSource random, NanobotSwarm exclusion) {
        if (this.effects.isEmpty()) throw new IllegalStateException("NanobotSwarm should always have effects!");

        Object2IntOpenHashMap<NanobotEffect> effects = new Object2IntOpenHashMap<>(this.effects);
        // reduce entries by those in exclusion
        for (Entry<NanobotEffect> entry : Object2IntMaps.fastIterable(exclusion.effects)) {
            effects.computeIntIfPresent(entry.getKey(), (e, i) -> {
                int newValue = i + entry.getIntValue();
                if (newValue > 0) return newValue;
                return null;
            });
        }

        return randomEffect(random, effects);
    }

    /// Returns null only if effects is empty
    @Nullable
    private static NanobotEffect randomEffect(RandomSource random, Object2IntMap<NanobotEffect> effects) {
        if (effects.isEmpty()) return null;
        if (effects.size() == 1) return effects.keySet().iterator().next();

        int weightedIndex = random.nextInt(effects.values().intStream().sum());

        Iterator<Entry<NanobotEffect>> entries = new ArrayList<>(effects.object2IntEntrySet()).iterator();
        Entry<NanobotEffect> t;
        do weightedIndex -= (t = entries.next()).getIntValue();
        while (weightedIndex >= 0 && entries.hasNext());

        return t.getKey();
    }

    public boolean hasEffect(TagKey<NanobotEffect> tag) {
        for (NanobotEffect value : effects.keySet()) {
            if (value.is(tag))
                return true;
        }
        return false;
    }

    private interface EffectConsumer {
        void accept(NanobotEffect effect, IAttachmentHolder host, int level);
    }

    private static void forEachEffect(Object2IntMap<NanobotEffect> effects, IAttachmentHolder host, EffectConsumer consumer) {
        for (Entry<NanobotEffect> entry : Object2IntMaps.fastIterable(effects)) {
            consumer.accept(entry.getKey(), host, entry.getIntValue());
        }
    }

    ///  Mark this as dirty when modified so it can be saved and resynced to clients
    void markDirty(IAttachmentHolder host) {
        host.setData(SWARM, this);
    }

    private static @NotNull Object2IntOpenCustomHashMap<NanobotEffect> newMap(int i) {
        return new Object2IntOpenCustomHashMap<>(i, IdentityStrategy.IDENTITY);
    }

    private NanobotSwarm(List<Entry<NanobotEffect>> effects) {
        this(newMap(effects.size()));
        for (var effect : effects) this.effects.put(effect.getKey(), effect.getIntValue());
    }

    private List<Entry<NanobotEffect>> apply(NanobotSwarm this) {
        return new ArrayList<>(this.effects.object2IntEntrySet());
    }

    @Override
    public String toString() {
        return effects.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NanobotSwarm that)) return false;

        return effects.equals(that.effects);
    }

    @Override
    public int hashCode() {
        return effects.hashCode();
    }
}
