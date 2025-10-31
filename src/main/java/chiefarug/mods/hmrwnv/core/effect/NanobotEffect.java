package chiefarug.mods.hmrwnv.core.effect;

import chiefarug.mods.hmrwnv.HfmrnvConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

import java.util.function.Function;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.EFFECT_CODEC_REG;

/// An interface representing an effect from nanobots.
/// Has some helper inner interfaces and records for composing common default methods
public interface NanobotEffect {
    Codec<NanobotEffect> CODEC = EFFECT_CODEC_REG.byNameCodec().dispatch(NanobotEffect::codec, Function.identity());

    /// The codec used for serializing this to disk
    /// Must be the same instance as stored in the registry.
    MapCodec<? extends NanobotEffect> codec();

    /// Called just after this effect is added to a nanobot swarm (or the swarm itself is added)
    /// Also called when the level changes, after {@link NanobotEffect#onRemove} has been called
    /// NOTE: Can be called if the host does not currently have this effect, in the case of entities entering chunks
    void onAdd(IAttachmentHolder host, int level);

    /// Called just before this effect is removed from a nanobot swarm (or the swarm itself is removed)
    /// Also called when the level changes, before {@link NanobotEffect#onAdd} has been called
    /// NOTE: Can be called if the host does not currently have this effect, in the case of entities leaving chunks
    void onRemove(IAttachmentHolder host, int level);

    /// Called frequently while this effect is part of a swarm is on something
    /// The exact rate is configurable, but by default is every tick for players and entities, and twice a second for chunks
    void onTick(IAttachmentHolder host, int level);

    /// Get tick rate for the given host type.
    static int getTickRate(IAttachmentHolder host) {
        return switch (host) {
            case Player ignored -> HfmrnvConfig.PLAYER_SLOW_DOWN_FACTOR.getAsInt();
            case LivingEntity ignored -> HfmrnvConfig.ENTITY_SLOW_DOWN_FACTOR.getAsInt();
            case ChunkAccess ignored -> HfmrnvConfig.CHUNK_SLOW_DOWN_FACTOR.getAsInt();
            default -> throw new IllegalArgumentException("Unknown host class " + host.getClass());
        };
    }

    /// Helper for making a nanobot effect that only ticks
    interface Ticking extends NanobotEffect {
        default void onAdd(IAttachmentHolder host, int level) {}
        default void onRemove(IAttachmentHolder host, int level) {}
    }

    /// Helper interface for making a nanobot effect that doesn't tick, but has a static effect that needs applying/removing.
    interface Stateful extends NanobotEffect {
        default void onTick(IAttachmentHolder holds, int level) {}
    }

    /// For effects that are implemented elsewhere (ie a mixin) so don't need overrides of any methods.
    /// Implementations like that should use a tag so that such effects can be stacked or changed if desired.
    final class Empty implements Ticking, Stateful {
        private static final Empty INSTANCE = new Empty();
        public static final MapCodec<Empty> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<Empty> codec() {
            return CODEC;
        }
    }
}
