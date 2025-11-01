package chiefarug.mods.hmrwnv.core.effect;

import chiefarug.mods.hmrwnv.HmrnvRegistries;
import chiefarug.mods.hmrwnv.core.EffectConfiguration;
import chiefarug.mods.hmrwnv.core.NanobotSwarm;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.EntitySection;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.INFECTION;
import static chiefarug.mods.hmrwnv.HmrnvRegistries.PROTECTS_AGAINST_SPREAD;
import static chiefarug.mods.hmrwnv.HmrnvRegistries.SWARM;

public record SpreadEffect(TypeConfiguration entityConfig, TypeConfiguration chunkConfig, int playerExposures, int entitySpreadDistance)
        implements NanobotEffect.Ticking, NanobotEffect.ChunkLocal {

    record TypeConfiguration(double chance, int exposures) {
        public static final MapCodec<TypeConfiguration> CODEC = RecordCodecBuilder.mapCodec(g -> g.group(
                Codec.DOUBLE.fieldOf("chance").forGetter(TypeConfiguration::chance),
                Codec.INT.fieldOf("exposures").forGetter(TypeConfiguration::exposures)
        ).apply(g, TypeConfiguration::new));
    }

    public static final MapCodec<SpreadEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                    TypeConfiguration.CODEC.fieldOf("entity_config").forGetter(SpreadEffect::entityConfig),
                    TypeConfiguration.CODEC.fieldOf("chunk_config").forGetter(SpreadEffect::chunkConfig),
                    Codec.INT.fieldOf("player_exposures").forGetter(SpreadEffect::playerExposures),
                    Codec.INT.fieldOf("entity_spread_distance").forGetter(SpreadEffect::entitySpreadDistance)
            ).apply(inst, SpreadEffect::new));

    @Override
    public MapCodec<? extends NanobotEffect> codec() {
        return CODEC;
    }

    @Override
    public void onTick(IAttachmentHolder host, int effectLevel) {
        if (host instanceof LivingEntity entity) {
            Level level = entity.level();
            if (level.getRandom().nextDouble() > Math.pow(entityConfig.chance, effectLevel)) {

                List<Entity> targets = level.getEntities(entity, entity.getBoundingBox().inflate(entitySpreadDistance));
                for (Entity target : targets) {
                    if (target instanceof Player)
                        infect(host, level, target, this.playerExposures);
                    else if (target instanceof LivingEntity)
                        infect(host, level, target, entityConfig.exposures);
                }

            }
        } else if (host instanceof LevelChunk chunk && chunk.getLevel() instanceof ServerLevel level) {
            if (level.getRandom().nextDouble() >= Math.pow(entityConfig.chance, effectLevel)) {
                level.entityManager.sectionStorage.getExistingSectionPositionsInChunk(chunk.getPos().toLong())
                        .mapToObj(level.entityManager.sectionStorage::getSection)
                        .filter(Objects::nonNull)
                        .flatMap(EntitySection::getEntities)
                        .filter(LivingEntity.class::isInstance)
                        .forEach(target -> infect(host, level, target, this.entitySpreadDistance));
            }
            if (level.getRandom().nextDouble() > Math.pow(chunkConfig.chance, effectLevel)) {
                Direction direction = Direction.values()[level.getRandom().nextInt(2, 6)];
                ChunkPos pos = chunk.getPos();
                LevelChunk target = level.getChunk(pos.x + direction.getStepX(), pos.z + direction.getStepZ());

                infect(chunk, level, target, chunkConfig.exposures);
            }
        }
    }

    private static void infect(IAttachmentHolder host, Level level, IAttachmentHolder target, int maxExposures) {
        Optional<NanobotSwarm> targetSwarm = target.getExistingData(SWARM);
        if (targetSwarm.isPresent() && targetSwarm.get().hasEffect(PROTECTS_AGAINST_SPREAD)) return;

        final Holder<EffectConfiguration<?>> effect;
        if (targetSwarm.isPresent())
            effect = host.getData(HmrnvRegistries.SWARM).randomEffectExcept(level.getRandom(), targetSwarm.get());
        else
            effect = host.getData(SWARM).randomEffect(level.getRandom());

        if (effect == null) return;

        ResourceLocation key = effect.getKey().location();
        Object2IntMap<ResourceLocation> infections = target.getData(INFECTION);
        int exposures = infections.mergeInt(key, 1, Integer::sum);
        if (exposures >= maxExposures) {
            NanobotSwarm.mergeSwarm(target, Object2IntMaps.singleton(effect, 1));
            infections.removeInt(effect);
            target.setData(INFECTION, infections);
        }
    }
}
