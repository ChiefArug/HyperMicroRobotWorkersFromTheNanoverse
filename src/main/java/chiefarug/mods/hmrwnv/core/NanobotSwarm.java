package chiefarug.mods.hmrwnv.core;

import chiefarug.mods.hmrwnv.client.ClientEffect;
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
import net.minecraft.core.Holder;
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
import static net.minecraft.SharedConstants.IS_RUNNING_IN_IDE;

/// Represents a swarm of nanobots that is hosted on some object
public final class NanobotSwarm {
    public static final Codec<Entry<Holder<EffectConfiguration<?>>>> ENTRY_CODEC = RecordCodecBuilder.create(inst1 -> inst1.group(
            //{
            // "effect" ResourceLocation[EffectConfiguration]
            EffectConfiguration.BY_ID_CODEC.fieldOf("effect").forGetter(Entry::getKey),
            // "level" int
            Codec.INT.fieldOf("level").forGetter(Entry::getIntValue)
            //}
    ).apply(inst1, BasicEntry::new));
    public static final Codec<Object2IntMap<Holder<EffectConfiguration<?>>>> EFFECTS_CODEC = Codec.list(ENTRY_CODEC)
            .xmap(c -> {
                    Object2IntOpenHashMap<Holder<EffectConfiguration<?>>> map = new Object2IntOpenHashMap<>();
                    if (IS_RUNNING_IN_IDE) c.forEach(e -> {if (e.getKey().value().effect() instanceof ClientEffect) throw new IllegalStateException("shouldn't be handling decoding these on client");});
                    for (Entry<Holder<EffectConfiguration<?>>> entry : c) map.put(entry.getKey(), entry.getIntValue());
                    return map;
                }, map -> ImmutableList.copyOf(map.object2IntEntrySet()));
    public static final Codec<NanobotSwarm> CODEC = EFFECTS_CODEC.xmap(NanobotSwarm::new, ns -> ns.effects).fieldOf("effects").codec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Object2IntMap<Holder<EffectConfiguration<?>>>> EFFECTS_STREAM_CODEC = ByteBufCodecs.map(NanobotSwarm::newMap, EffectConfiguration.HOLDER_STREAM_CODEC, ByteBufCodecs.INT);
    public static final StreamCodec<RegistryFriendlyByteBuf, NanobotSwarm> STREAM_CODEC = EFFECTS_STREAM_CODEC.map(NanobotSwarm::new, NanobotSwarm::getEffects);
    //TODO: migrate this to a custom class so the type is much shorter
    private final Object2IntMap<Holder<EffectConfiguration<?>>> effects;

    private NanobotSwarm(Map<Holder<EffectConfiguration<?>>, Integer> effects) {
        this.effects = new Object2IntOpenCustomHashMap<>(effects, IdentityStrategy.IDENTITY);
    }

    /// Attach a new swarm to this host. Replaces an existing swarm on that host, if present.
    public static NanobotSwarm attachSwarm(IAttachmentHolder host, Map<Holder<EffectConfiguration<?>>, Integer> effects) {
        if (IS_RUNNING_IN_IDE) {
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
    public static void mergeSwarm(IAttachmentHolder host, Object2IntMap<Holder<EffectConfiguration<?>>> inEffects) {
        Optional<NanobotSwarm> existing = host.getExistingData(SWARM);
        if (existing.isPresent()) {
            NanobotSwarm existingSwarm = existing.get();

            // these are used to build an array map
            Holder<EffectConfiguration<?>>[] newKeys = new Holder[inEffects.size()];
            int[] newValues = new int[inEffects.size()];
            int i = 0;

            // copy over the entries that are higher level
            for (Entry<Holder<EffectConfiguration<?>>> entry : Object2IntMaps.fastIterable(inEffects)) {
                Holder<EffectConfiguration<?>> effect = entry.getKey();
                int level = entry.getIntValue();
                int existingLevel = existingSwarm.getEffectLevel(effect.value()).orElse(0);

                // if the existing level is less the new level, add it to the arrays to merge in
                // and if its not 0, run its remove trigger
                if (existingLevel < level) {
                    if (existingLevel != 0)
                        effect.value().onRemove(host, level);
                    newKeys[i] = effect;
                    newValues[i++] = level;
                }
            }

            Object2IntArrayMap<Holder<EffectConfiguration<?>>> newEffects = new Object2IntArrayMap<>(newKeys, newValues, i);
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

        mayExist.get().beforeRemove(host);

        host.removeData(SWARM);
    }

    @UnmodifiableView
    public Object2IntMap<Holder<EffectConfiguration<?>>> getEffects() {
        return Object2IntMaps.unmodifiable(effects);
    }

    /// Add or updates a single effect to this swarm with the specified level.
    public void addEffect(IAttachmentHolder host, Holder<EffectConfiguration<?>> effect, int level) {
        effects.put(effect, level);
        effect.value().onAdd(host, level);
        markDirty(host);
    }

    /// Add multiple effects to this swarm at once. This is preferred over repeatedly calling {@link NanobotSwarm#addEffect} as it only syncs once.
    /// As a bonus it also batches operations, so all additions can see all the other additions in {@link NanobotEffect#onAdd}
    public void addEffects(IAttachmentHolder host, Object2IntMap<Holder<EffectConfiguration<?>>> newEffects) {
        effects.putAll(newEffects);
        forEachEffect(newEffects, host, EffectConfiguration::onAdd);
        markDirty(host);
    }

    /// Removes a single effect from this swarm. Does nothing if the effect is not part of this swarm.
    public void removeEffect(IAttachmentHolder host, Holder<EffectConfiguration<?>> effect) {
        if (!effects.containsKey(effect))
            return;
        effect.value().onRemove(host, effects.getInt(effect));
        effects.removeInt(effect);
        markDirty(host);
    }

    /// Removes multiple effects from this swarm at once. This is preferred over repeatedly calling {@link NanobotSwarm#removeEffect} as it only syncs once.
    /// As a bonus it also batches operations, so all removals can see all the other removals in {@link NanobotEffect#onRemove}.
    public void removeEffects(IAttachmentHolder host, Collection<Holder<EffectConfiguration<?>>> oldEffects) {
        for (Holder<EffectConfiguration<?>> effect : oldEffects) {
            if (!effects.containsKey(effect)) continue;
            effect.value().onRemove(host, effects.getInt(effect));
        }
        for (Holder<EffectConfiguration<?>> oldEffect : oldEffects) {
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
                .mapToInt(e -> e.getKey().value().getRequiredPower(e.getIntValue()))
                .sum();
    }

    /// Get a random effect weighted by effect levels
    @NotNull
    public Holder<EffectConfiguration<?>> randomEffect(RandomSource random) {
        if (effects.isEmpty()) throw new IllegalStateException("NanobotSwarm should always have effects!");

        // not null cause we assert its not empty, and it only returns null on empty maps.
        //noinspection DataFlowIssue
        return randomEffect(random, effects);
    }

    @Nullable
    public Holder<EffectConfiguration<?>> randomEffectExcept(RandomSource random, NanobotSwarm exclusion) {
        if (this.effects.isEmpty()) throw new IllegalStateException("NanobotSwarm should always have effects!");

        Object2IntOpenHashMap<Holder<EffectConfiguration<?>>> effects = new Object2IntOpenHashMap<>(this.effects);
        // reduce entries by those in exclusion
        for (Entry<Holder<EffectConfiguration<?>>> entry : Object2IntMaps.fastIterable(exclusion.effects)) {
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
    private static Holder<EffectConfiguration<?>> randomEffect(RandomSource random, Object2IntMap<Holder<EffectConfiguration<?>>> effects) {
        if (effects.isEmpty()) return null;
        if (effects.size() == 1) return effects.keySet().iterator().next();

        int weightedIndex = random.nextInt(effects.values().intStream().sum());

        Iterator<Entry<Holder<EffectConfiguration<?>>>> entries = new ArrayList<>(effects.object2IntEntrySet()).iterator();
        Entry<Holder<EffectConfiguration<?>>> t;
        do weightedIndex -= (t = entries.next()).getIntValue();
        while (weightedIndex >= 0 && entries.hasNext());

        return t.getKey();
    }

    public boolean hasEffect(TagKey<EffectConfiguration<?>> tag) {
        for (Holder<EffectConfiguration<?>> value : effects.keySet()) {
            if (value.is(tag))
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

    public static void forEachEffect(Object2IntMap<Holder<EffectConfiguration<?>>> effects, IAttachmentHolder host, EffectConsumer consumer) {
        for (Entry<Holder<EffectConfiguration<?>>> entry : Object2IntMaps.fastIterable(effects)) {
            consumer.accept(entry.getKey().value(), host, entry.getIntValue());
        }
    }

    ///  Mark this as dirty when modified so it can be saved and resynced to clients
    void markDirty(IAttachmentHolder host) {
        host.setData(SWARM, this);
    }

    private static @NotNull Object2IntOpenCustomHashMap<Holder<EffectConfiguration<?>>> newMap(int i) {
        return new Object2IntOpenCustomHashMap<>(i, IdentityStrategy.IDENTITY);
    }

    private NanobotSwarm(List<Entry<Holder<EffectConfiguration<?>>>> effects) {
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
