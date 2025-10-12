package chiefarug.mods.hfmrwnv.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

public record NanobotSwarm(ResourceLocation id, List<NanobotEffect> effects) {
    public static final Codec<NanobotSwarm> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                    ResourceLocation.CODEC.fieldOf("id").forGetter(NanobotSwarm::id),
                    NanobotEffect.CODEC.listOf().fieldOf("effects").forGetter(NanobotSwarm::effects)
            ).apply(inst, NanobotSwarm::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, NanobotSwarm> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, NanobotSwarm::id,
            id1 -> new NanobotSwarm(id1, List.of())
    );

    public void tickChunk(ServerLevel level, LevelChunk holder) {

    }

    public void tickEntity(ServerLevel level, Entity holder) {

    }

    public void tickPlayer(ServerLevel level, ServerPlayer holder) {
        tickEntity(level, holder);
    }

}
