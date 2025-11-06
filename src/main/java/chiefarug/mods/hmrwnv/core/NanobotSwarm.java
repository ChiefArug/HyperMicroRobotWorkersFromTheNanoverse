package chiefarug.mods.hmrwnv.core;

import chiefarug.mods.hmrwnv.HfmrnvConfig;
import chiefarug.mods.hmrwnv.HmrnvRegistries;
import chiefarug.mods.hmrwnv.core.collections.EffectArrayMap;
import chiefarug.mods.hmrwnv.core.collections.EffectMap;
import chiefarug.mods.hmrwnv.core.effect.NanobotEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
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
    private static final int NOT_DISMANTLING = -1;
    public static final Codec<NanobotSwarm> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            EffectMap.CODEC.fieldOf("effects").forGetter(n -> n.effects),
            Codec.INT.optionalFieldOf("dismantling").xmap(o -> o.orElse(NOT_DISMANTLING), i -> i == NOT_DISMANTLING ? Optional.empty() : Optional.of(i)).forGetter(n -> n.dismantlingTime)
    ).apply(inst, NanobotSwarm::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, NanobotSwarm> STREAM_CODEC = EffectMap.EFFECTS_STREAM_CODEC.map(NanobotSwarm::new, NanobotSwarm::getEffects);
    private final EffectMap effects;
    private int dismantlingTime;

    private NanobotSwarm(Map<Holder<EffectConfiguration<?>>, Integer> effects, int dismantlingTime) {
        this.effects = new EffectArrayMap(effects);
        this.dismantlingTime = dismantlingTime;
    }

    private NanobotSwarm(Map<Holder<EffectConfiguration<?>>, Integer> effects) {
        this(effects, NOT_DISMANTLING);
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
        // the swarm may instantly start dismantling if it is unstable
        swarm.maybeStartDismantling(host);
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
                // and if it's not 0, run its remove trigger
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
            // if the swarm is now unstable it will start dismantling itself
            existingSwarm.maybeStartDismantling(host);
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
        // if we are dismantling then we tick that, and if it finishes dismantling then return from here
        if (isDismantling() && tickDismantling(host)) return;

        this.forEachEffect(host, EffectConfiguration::onTick);
    }

    public OptionalInt getEffectLevel(Holder<EffectConfiguration<?>> effect) {
        return effects.containsKey(effect) ? OptionalInt.of(effects.getInt(effect)) : OptionalInt.empty();
    }

    private void maybeStartDismantling(IAttachmentHolder host) {
        if (!isDismantling() && !hasEnoughPower()) {
            // start dismantling
            dismantlingTime = HfmrnvConfig.TICKS_TO_DISMANTLE.getAsInt();
            markDirty(host);
        }
    }

    /// This assumes isDismantling() returns true
    /// @return true if this swarm finished dismantling
    private boolean tickDismantling(IAttachmentHolder host) {
        if (hasEnoughPower()) {
            // cancel dismantling
            dismantlingTime = NOT_DISMANTLING;
            markDirty(host);
            return false;
        }

        dismantlingTime -= NanobotEffect.getTickRate(host);
        if (dismantlingTime <= 0) {
            // we have finished dismantling
            this.forEachEffect(host, EffectConfiguration::onRemove);
            effects.clear();
            host.removeData(SWARM);
            ServerLevel level = level(host);
            if (level != null) {
                Vec3 pos = position(host);
                level.playSound(null, pos.x, pos.y, pos.z, HmrnvRegistries.NANOBOT_DISMANTLE.get(), SoundSource.AMBIENT);
            }
            return true;
        } else {
            // we must mark dirty otherwise the dismantlingTime won't save
            markDirty(host);
            return false;
        }
    }

    public boolean isDismantling() {
        return dismantlingTime >= 0;
    }

    public boolean hasEnoughPower() {
        return effects.totalPower() <= 0;
    }

    /// Get a random effect weighted by effect levels
    public Holder<EffectConfiguration<?>> randomEffect(RandomSource random) {
        if (effects.isEmpty()) throw new IllegalStateException("NanobotSwarm should always have effects!");

        // not null cause we assert it's not empty, and it only returns null on empty maps.
        //noinspection DataFlowIssue
        return randomEffect(random, effects);
    }

    /// Get a random effect that is not already a part of the provided swarm
    @Nullable
    public Holder<EffectConfiguration<?>> randomEffectExcept(RandomSource random, EffectMap exclusion) {
        if (this.effects.isEmpty()) throw new IllegalStateException("NanobotSwarm should always have effects!");

        EffectMap effects = new EffectArrayMap(this.effects);
        // reduce entries by those in exclusion
        for (Entry<Holder<EffectConfiguration<?>>> entry : Object2IntMaps.fastIterable(exclusion)) {
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
    public static Holder<EffectConfiguration<?>> randomEffect(RandomSource random, EffectMap effects) {
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

    /// Returns true if at least one of the effects on this swarm has the provided tag
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
        return effects.toString() + ", dismantling=" + dismantlingTime;
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

    @Nullable
    private static ServerLevel level(IAttachmentHolder host) {
        return switch (host) {
            case Entity e -> e.level();
            case LevelChunk c -> c.getLevel();
            default -> null;
        } instanceof ServerLevel sl ? sl : null;
    }

    private static Vec3 position(IAttachmentHolder host) {
        return switch (host) {
            case Entity e -> e.position();
            case LevelChunk c -> {
                Level level = c.getLevel();
                int midX = c.getPos().getMiddleBlockX();
                int midZ = c.getPos().getMiddleBlockZ();
                int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, midX, midZ);
                yield new Vec3(midX + 0.5, y, midZ + 0.5);
            }
            default -> throw new IllegalArgumentException("Unknown host class: " + host.getClass());
        };
    }
}
