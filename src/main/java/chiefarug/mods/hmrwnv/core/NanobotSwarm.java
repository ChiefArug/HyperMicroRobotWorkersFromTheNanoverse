package chiefarug.mods.hmrwnv.core;

import chiefarug.mods.hmrwnv.core.effect.NanobotEffect;
import com.google.common.collect.ImmutableList;
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
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.util.strategy.IdentityStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.SWARM;

/// Represents a swarm of nanobots that is hosted on some object
public final class NanobotSwarm {
    public static final Codec<Entry<EffectConfiguration<?>>> ENTRY_CODEC = RecordCodecBuilder.create(inst1 -> inst1.group(
            //{
            // "effect" ResourceLocation[EffectConfiguration]
            EffectConfiguration.BY_ID_CODEC.fieldOf("effect").forGetter(Entry::getKey),
            // "level" int
            Codec.INT.fieldOf("level").forGetter(Entry::getIntValue)
            //}
    ).apply(inst1, BasicEntry::new));
    public static final Codec<Object2IntMap<EffectConfiguration<?>>> EFFECTS_CODEC = Codec.list(ENTRY_CODEC)
            .xmap(c -> {
                    Object2IntOpenHashMap<EffectConfiguration<?>> map = new Object2IntOpenHashMap<>();
                    for (Entry<EffectConfiguration<?>> entry : c) map.put(entry.getKey(), entry.getIntValue());
                    return map;
                }, map -> ImmutableList.copyOf(map.object2IntEntrySet()));
    public static final Codec<NanobotSwarm> CODEC = EFFECTS_CODEC.xmap(NanobotSwarm::new, ns -> ns.effects).fieldOf("effects").codec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Object2IntMap<EffectConfiguration<?>>> EFFECTS_STREAM_CODEC = ByteBufCodecs.map(NanobotSwarm::newMap, EffectConfiguration.STREAM_CODEC, ByteBufCodecs.INT);
    public static final StreamCodec<RegistryFriendlyByteBuf, NanobotSwarm> STREAM_CODEC = EFFECTS_STREAM_CODEC.map(NanobotSwarm::new, NanobotSwarm::getEffects);

    private final Object2IntMap<EffectConfiguration<?>> effects;

    private NanobotSwarm(Map<EffectConfiguration<?>, Integer> effects) {
        this.effects = new Object2IntOpenCustomHashMap<>(effects, IdentityStrategy.IDENTITY);
    }

    /// Attach a new swarm to this host. Replaces an existing swarm on that host, if present.
    public static NanobotSwarm attachSwarm(IAttachmentHolder host, Map<EffectConfiguration<?>, Integer> effects) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            switch (host) {
                case Player ignored -> {}
                case LivingEntity ignored -> {}
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
    public static void mergeSwarm(IAttachmentHolder host, Object2IntMap<EffectConfiguration<?>> inEffects) {
        Optional<NanobotSwarm> existing = host.getExistingData(SWARM);
        if (existing.isPresent()) {
            NanobotSwarm existingSwarm = existing.get();

            // these are used to build an array map
            Object[] newKeys = new Object[inEffects.size()];
            int[] newValues = new int[inEffects.size()];
            int i = 0;

            // copy over the entries that are higher level
            for (Entry<EffectConfiguration<?>> entry : Object2IntMaps.fastIterable(inEffects)) {
                EffectConfiguration<?> effect = entry.getKey();
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

            Object2IntArrayMap<EffectConfiguration<?>> newEffects = new Object2IntArrayMap<>(newKeys, newValues, i);
            existingSwarm.effects.putAll(newEffects);

            forEachEffect(newEffects, host, EffectConfiguration::onAdd);
            existingSwarm.markDirty(host);
        } else {
            attachSwarm(host, inEffects);
        }
    }

    /// Clears any swarm on the host
    public static void clearSwarm(IAttachmentHolder host) {
        Optional<NanobotSwarm> mayExist = host.getExistingData(SWARM);
        if (mayExist.isEmpty()) return;

        forEachEffect(mayExist.get().effects, host, EffectConfiguration::onRemove);

        host.removeData(SWARM);
    }

    @UnmodifiableView
    public Object2IntMap<EffectConfiguration<?>> getEffects() {
        return Object2IntMaps.unmodifiable(effects);
    }

    /// Add or updates a single effect to this swarm with the specified level.
    public void addEffect(IAttachmentHolder host, EffectConfiguration<?> effect, int level) {
        effects.put(effect, level);
        effect.onAdd(host, level);
        markDirty(host);
    }

    /// Add multiple effects to this swarm at once. This is preferred over repeatedly calling {@link NanobotSwarm#addEffect} as it only syncs once.
    /// As a bonus it also batches operations, so all additions can see all the other additions in {@link NanobotEffect#onAdd}
    public void addEffects(IAttachmentHolder host, Object2IntMap<EffectConfiguration<?>> newEffects) {
        effects.putAll(newEffects);
        forEachEffect(newEffects, host, EffectConfiguration::onAdd);
        markDirty(host);
    }

    /// Removes a single effect from this swarm. Does nothing if the effect is not part of this swarm.
    public void removeEffect(IAttachmentHolder host, EffectConfiguration<?> effect) {
        if (!effects.containsKey(effect))
            return;
        effect.onRemove(host, effects.getInt(effect));
        effects.removeInt(effect);
        markDirty(host);
    }

    /// Removes multiple effects from this swarm at once. This is preferred over repeatedly calling {@link NanobotSwarm#removeEffect} as it only syncs once.
    /// As a bonus it also batches operations, so all removals can see all the other removals in {@link NanobotEffect#onRemove}.
    public void removeEffects(IAttachmentHolder host, Collection<EffectConfiguration<?>> oldEffects) {
        for (EffectConfiguration<?> effect : oldEffects) {
            if (!effects.containsKey(effect)) continue;
            effect.onRemove(host, effects.getInt(effect));
        }
        for (EffectConfiguration<?> oldEffect : oldEffects) {
            effects.removeInt(oldEffect);
        }
        markDirty(host);
    }

    /// Called once server-side when this swarm is first added to a host.
    public void afterAdd(IAttachmentHolder host) {
        forEachEffect(effects, host, EffectConfiguration::onAdd);
    }

    /// Called once server-side just before this swarm is removed from a host
    public void beforeRemove(IAttachmentHolder host) {
        forEachEffect(effects, host, EffectConfiguration::onRemove);
    }

    /// Called frequently server-side while this effect is part of a swarm is on something.
    /// The exact rate is configurable, but by default is every tick for players and entities, and twice a second for chunks.
    public void tick(IAttachmentHolder host) {
        forEachEffect(effects, host, EffectConfiguration::onTick);
    }

    public OptionalInt getEffectLevel(EffectConfiguration<?> effect) {
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
    public EffectConfiguration<?> randomEffect(RandomSource random) {
        if (effects.isEmpty()) throw new IllegalStateException("NanobotSwarm should always have effects!");

        // not null cause we assert its not empty, and it only returns null on empty maps.
        //noinspection DataFlowIssue
        return randomEffect(random, effects);
    }

    @Nullable
    public EffectConfiguration<?> randomEffectExcept(RandomSource random, NanobotSwarm exclusion) {
        if (this.effects.isEmpty()) throw new IllegalStateException("NanobotSwarm should always have effects!");

        Object2IntOpenHashMap<EffectConfiguration<?>> effects = new Object2IntOpenHashMap<>(this.effects);
        // reduce entries by those in exclusion
        for (Entry<EffectConfiguration<?>> entry : Object2IntMaps.fastIterable(exclusion.effects)) {
            effects.computeIntIfPresent(entry.getKey(), (e, i) -> {
                int newValue = i - entry.getIntValue();
                if (newValue > 0) return newValue;
                return null;
            });
        }

        return randomEffect(random, effects);
    }

    /// Returns null only if effects is empty
    @Nullable
    private static EffectConfiguration<?> randomEffect(RandomSource random, Object2IntMap<EffectConfiguration<?>> effects) {
        if (effects.isEmpty()) return null;
        if (effects.size() == 1) return effects.keySet().iterator().next();

        int weightedIndex = random.nextInt(effects.values().intStream().sum());

        Iterator<Entry<EffectConfiguration<?>>> entries = new ArrayList<>(effects.object2IntEntrySet()).iterator();
        Entry<EffectConfiguration<?>> t;
        do weightedIndex -= (t = entries.next()).getIntValue();
        while (weightedIndex >= 0 && entries.hasNext());

        return t.getKey();
    }

    public boolean hasEffect(RegistryAccess access, TagKey<EffectConfiguration<?>> tag) {
        for (EffectConfiguration<?> value : effects.keySet()) {
            if (value.is(access, tag))
                return true;
        }
        return false;
    }

    public interface EffectConsumer {
        void accept(EffectConfiguration<?> effect, IAttachmentHolder host, int level);
    }

    public void forEachEffect(IAttachmentHolder host, EffectConsumer consumer) {
        forEachEffect(effects, host, consumer);
    }

    public static void forEachEffect(Object2IntMap<EffectConfiguration<?>> effects, IAttachmentHolder host, EffectConsumer consumer) {
        for (Entry<EffectConfiguration<?>> entry : Object2IntMaps.fastIterable(effects)) {
            consumer.accept(entry.getKey(), host, entry.getIntValue());
        }
    }

    ///  Mark this as dirty when modified so it can be saved and resynced to clients
    void markDirty(IAttachmentHolder host) {
        host.setData(SWARM, this);
    }

    private static @NotNull Object2IntOpenCustomHashMap<EffectConfiguration<?>> newMap(int i) {
        return new Object2IntOpenCustomHashMap<>(i, IdentityStrategy.IDENTITY);
    }

    private NanobotSwarm(List<Entry<EffectConfiguration<?>>> effects) {
        this(newMap(effects.size()));
        for (var effect : effects) this.effects.put(effect.getKey(), effect.getIntValue());
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
