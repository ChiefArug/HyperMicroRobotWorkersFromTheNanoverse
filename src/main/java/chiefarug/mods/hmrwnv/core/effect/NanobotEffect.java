package chiefarug.mods.hmrwnv.core.effect;

import chiefarug.mods.hmrwnv.HfmrnvConfig;
import chiefarug.mods.hmrwnv.HmrnvRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/// An interface representing an effect from nanobots.
/// Has some sub-interfaces for composing common default methods
public interface NanobotEffect {
    Codec<NanobotEffect> CODEC = HmrnvRegistries.EFFECT.byNameCodec().dispatch(Function.identity(), NanobotEffect::codec);
    // Only sync the id over the network, not the full object.
    StreamCodec<RegistryFriendlyByteBuf, NanobotEffect> STREAM_CODEC = ByteBufCodecs.registry(HmrnvRegistries.EFFECT.key());

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

    default boolean is(TagKey<NanobotEffect> tag) {
        return HmrnvRegistries.EFFECT.wrapAsHolder(this).is(tag);
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

    ///  Helper interface for an effect that has no constructor parameters so can use a Unit codec.
    interface Unit extends NanobotEffect {
        default MapCodec<? extends NanobotEffect> codec() {
            return MapCodec.unit(this);
        }
    }

    /// Helper record for effects that are implemented elsewhere (ie a mixin) so don't need overrides of any methods.
    record Static(IntUnaryOperator levelToPower) implements Unit, NonStateful, NonTicking {
        public Static(int powerPerLevel) { this(i -> i * powerPerLevel); }

        @Override
        public int getRequiredPower(int level) {
            return levelToPower.applyAsInt(level);
        }
    }
}
