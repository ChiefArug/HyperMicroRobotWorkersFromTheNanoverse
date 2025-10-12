package chiefarug.mods.hfmrwnv.core.effect;

import chiefarug.mods.hfmrwnv.HfmrnvConfig;
import chiefarug.mods.hfmrwnv.HfmrnvRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

import java.util.function.Function;

/// An interface representing an effect from nanobots.
public interface NanobotEffect {
    Codec<NanobotEffect> CODEC = HfmrnvRegistries.EFFECTS.byNameCodec().dispatch(Function.identity(), NanobotEffect::codec);
    // Only sync the id over the network, not the full object.
    StreamCodec<RegistryFriendlyByteBuf, NanobotEffect> STREAM_CODEC = ByteBufCodecs.registry(HfmrnvRegistries.EFFECTS.key());

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

    static int getTickRate(IAttachmentHolder host) {
        return switch (host) {
            case Player ignored -> HfmrnvConfig.PLAYER_SLOW_DOWN_FACTOR.getAsInt();
            case Entity ignored -> HfmrnvConfig.ENTITY_SLOW_DOWN_FACTOR.getAsInt();
            case ChunkAccess ignored -> HfmrnvConfig.CHUNK_SLOW_DOWN_FACTOR.getAsInt();
            default -> throw new IllegalArgumentException("Unknown host class " + host.getClass());
        };
    }

    interface Ticking extends NanobotEffect {
        default void onAdd(IAttachmentHolder host, int level) {}
        default void onRemove(IAttachmentHolder host, int level) {}
    }

    interface Constant extends NanobotEffect {
        default void onTick(IAttachmentHolder holds, int level) {}
    }
}
