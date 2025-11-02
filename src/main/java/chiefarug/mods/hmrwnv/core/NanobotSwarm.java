package chiefarug.mods.hmrwnv.core;

import chiefarug.mods.hmrwnv.core.collections.EffectArrayMap;
import chiefarug.mods.hmrwnv.core.collections.EffectMap;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.SWARM;
import static net.minecraft.SharedConstants.IS_RUNNING_IN_IDE;

/// Represents a swarm of nanobots that is hosted on some object
public final class NanobotSwarm {
    public static final Codec<NanobotSwarm> CODEC = EffectMap.CODEC.xmap(NanobotSwarm::new, ns -> ns.effects).fieldOf("effects").codec();
    public static final StreamCodec<RegistryFriendlyByteBuf, NanobotSwarm> STREAM_CODEC = EffectMap.EFFECTS_STREAM_CODEC.map(NanobotSwarm::new, NanobotSwarm::getEffects);
    private final EffectMap effects;

    private NanobotSwarm(Map<Holder<EffectConfiguration<?>>, Integer> effects) {
        this.effects = new EffectArrayMap(effects);
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
        if (swarm != null) swarm.forEachEffect(host, EffectConfiguration::onRemove);

        swarm = new NanobotSwarm(effects);
        host.setData(SWARM, swarm);
        swarm.forEachEffect(host, EffectConfiguration::onAdd);
        return swarm;
    }

    /// Merge these effects into the host, swarming them if they are not already swarmed otherwise adding any effects that are a higher level
    public static void mergeSwarm(IAttachmentHolder host, EffectMap inEffects) {
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
                int existingLevel = existingSwarm.getEffectLevel(effect).orElse(0);

                // if the existing level is less the new level, add it to the arrays to merge in
                // and if its not 0, run its remove trigger
                if (existingLevel < level) {
                    if (existingLevel != 0)
                        effect.value().onRemove(host, level);
                    newKeys[i] = effect;
                    newValues[i++] = level;
                }
            }

            EffectMap newEffects = new EffectArrayMap(newKeys, newValues, i);
            existingSwarm.effects.putAll(newEffects);

            forEachEffect(newEffects, host, EffectConfiguration::onAdd);
            existingSwarm.markDirty(host);
        } else {
            attachSwarm(host, inEffects);
        }
    }

    @UnmodifiableView
    public EffectMap getEffects() {
        return EffectMap.unmodifiable(effects);
    }


    /// Called frequently server-side while this effect is part of a swarm is on something.
    /// The exact rate is configurable, but by default is every tick for players and entities, and twice a second for chunks.
    public void tick(IAttachmentHolder host) {
        this.forEachEffect(host, EffectConfiguration::onTick);
    }

    public OptionalInt getEffectLevel(Holder<EffectConfiguration<?>> effect) {
        return effects.containsKey(effect) ? OptionalInt.of(effects.getInt(effect)) : OptionalInt.empty();
    }

    public boolean hasEnoughPower() {
        return getPowerTotal() >= 0;
    }

    private int getPowerTotal() {
        return effects.totalPower();
    }

    /// Get a random effect weighted by effect levels
    public Holder<EffectConfiguration<?>> randomEffect(RandomSource random) {
        if (effects.isEmpty()) throw new IllegalStateException("NanobotSwarm should always have effects!");

        // not null cause we assert its not empty, and it only returns null on empty maps.
        //noinspection DataFlowIssue
        return randomEffect(random, effects);
    }

    @Nullable
    public Holder<EffectConfiguration<?>> randomEffectExcept(RandomSource random, NanobotSwarm exclusion) {
        if (this.effects.isEmpty()) throw new IllegalStateException("NanobotSwarm should always have effects!");

        EffectMap effects = new EffectArrayMap(this.effects);
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
    private static Holder<EffectConfiguration<?>> randomEffect(RandomSource random, EffectMap effects) {
        if (effects.isEmpty()) return null;
        if (effects.size() == 1) return effects.keySet().iterator().next();

        int weightedIndex = random.nextInt(effects.values().intStream().sum());

        // TODO: this probably shouldn't be weighted as it disadvantages single level effects like night vision
        Iterator<Entry<Holder<EffectConfiguration<?>>>> entries = effects.asUnmodifiableList().iterator();
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

    public static void forEachEffect(EffectMap effects, IAttachmentHolder host, EffectConsumer consumer) {
        //TODO: replace this with a version that doesn't use allocation
        for (Entry<Holder<EffectConfiguration<?>>> entry : Object2IntMaps.fastIterable(effects)) {
            consumer.accept(entry.getKey().value(), host, entry.getIntValue());
        }
    }

    ///  Mark this as dirty when modified so it can be saved and resynced to clients
    void markDirty(IAttachmentHolder host) {
        host.setData(SWARM, this);
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
