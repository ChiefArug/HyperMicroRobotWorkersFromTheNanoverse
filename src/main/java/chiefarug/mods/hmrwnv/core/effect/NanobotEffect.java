package chiefarug.mods.hmrwnv.core.effect;

import chiefarug.mods.hmrwnv.HfmrnvConfig;
import chiefarug.mods.hmrwnv.RegistryInjectionXMapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

import java.util.function.Function;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.EFFECTS_KEY;
import static chiefarug.mods.hmrwnv.HmrnvRegistries.EFFECT_CODEC_REG;
import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODRL_CODEC;

/// An interface representing an effect from nanobots.
/// Has some sub-interfaces for composing common default methods
public interface NanobotEffect {

    Codec<NanobotEffect> CODEC = EFFECT_CODEC_REG.byNameCodec().dispatch(NanobotEffect::codec, Function.identity());
    Codec<NanobotEffect> BY_ID_CODEC = RegistryInjectionXMapCodec.createRegistryValue(MODRL_CODEC, EFFECTS_KEY);//RegistryFixedCodec.create(EFFECTS).xmap(Holder::value, Holder::direct); // pls dont break TODO: it broke, direct holders dont like sending over network.
    StreamCodec<? super RegistryFriendlyByteBuf, NanobotEffect> STREAM_CODEC = ByteBufCodecs.registry(EFFECTS_KEY);
    MapCodec<Integer> LEVEL_MULTIPLIER = Codec.INT.fieldOf("energy_multiplier");

    /// The codec used for serializing this to disk
    MapCodec<? extends NanobotEffect> codec();

    /// Called just after this effect is added to a nanobot swarm (or the swarm itself is added)
    /// Also called when the level changes, after {@link NanobotEffect#onRemove} has been called
    void onAdd(IAttachmentHolder host, int level);

    /// Called just before this effect is removed from a nanobot swarm (or the swarm itself is removed)
    /// Also called when the level changes, before {@link NanobotEffect#onAdd} has been called
    void onRemove(IAttachmentHolder host, int level);

    /// Called frequently while this effect is part of a swarm is on something
    /// The exact rate is configurable, but by default is every tick for players and entities, and evey second for chunks
    void onTick(IAttachmentHolder host, int level);

    /// Get the power that this requires/provides. If it provides power the return value should be negative
    int getRequiredPower(int level);

    static int getTickRate(IAttachmentHolder host) {
        return switch (host) {
            case Player ignored -> HfmrnvConfig.PLAYER_SLOW_DOWN_FACTOR.getAsInt();
            case LivingEntity ignored -> HfmrnvConfig.ENTITY_SLOW_DOWN_FACTOR.getAsInt();
            case ChunkAccess ignored -> HfmrnvConfig.CHUNK_SLOW_DOWN_FACTOR.getAsInt();
            default -> throw new IllegalArgumentException("Unknown host class " + host.getClass());
        };
    }

    static Registry<NanobotEffect> reg(RegistryAccess access) {
        return access.registryOrThrow(EFFECTS_KEY);
    }

    default boolean is(RegistryAccess access, TagKey<NanobotEffect> tag) {
        return reg(access).wrapAsHolder(this).is(tag);
    }

    default ResourceLocation id(RegistryAccess access) {
        return reg(access).getKey(this);
    }

    default MutableComponent name(RegistryAccess access) {
        return Component.translatable(id(access).toLanguageKey(EFFECTS_KEY.location().getPath()));
    }

    default MutableComponent description(RegistryAccess access) {
        return Component.translatable(id(access).toLanguageKey(EFFECTS_KEY.location().getPath()) + ".description");
    }

    default MutableComponent nameWithLevel(RegistryAccess access, int level) {
        return Component.translatable("hmrw_nanoverse.effect_level.formatting",
                name(access), level(level));
    }

    static MutableComponent level(int i) {
        return Component.translatable("hmrw_nanoverse.effect_level." + i);
    }

    /// Helper for making a nanobot effect that only ticks/doesnt use onAdd/onRemove
    interface NonStateful extends NanobotEffect {
        default void onAdd(IAttachmentHolder host, int level) {}
        default void onRemove(IAttachmentHolder host, int level) {}
    }

    /// Helper interface for making a nanobot effect that doesn't tick, but has a static effect that needs applying/removing.
    interface NonTicking extends NanobotEffect {
        default void onTick(IAttachmentHolder holds, int level) {}
    }

    /// Helper record for effects that are implemented elsewhere (ie a mixin) so don't need overrides of any methods.
    record Static(int levelMultiplier) implements NonStateful, NonTicking {
        public static final MapCodec<Static> CODEC = LEVEL_MULTIPLIER.xmap(Static::new, Static::levelMultiplier);

        @Override
        public MapCodec<NanobotEffect.Static> codec() {
            return CODEC;
        }

        @Override
        public int getRequiredPower(int level) {
            return levelMultiplier * level;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override // referential equality
        public boolean equals(Object obj) {
            return this == obj;
        }
    }
}
