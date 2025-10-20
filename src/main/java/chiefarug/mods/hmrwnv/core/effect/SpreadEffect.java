package chiefarug.mods.hmrwnv.core.effect;

import chiefarug.mods.hmrwnv.HfmrnvConfig;
import chiefarug.mods.hmrwnv.HmrnvRegistries;
import chiefarug.mods.hmrwnv.core.NanobotSwarm;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.EntitySection;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntSupplier;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.INFECTION;
import static chiefarug.mods.hmrwnv.HmrnvRegistries.PROTECTS_AGAINST_SPREAD;
import static chiefarug.mods.hmrwnv.HmrnvRegistries.SWARM;

public class SpreadEffect implements NanobotEffect.NonStateful, NanobotEffect.Unit {

    @Override
    public void onTick(IAttachmentHolder host, int effectLevel) {
        if (host instanceof Entity entity) {
            Level level = entity.level();
            if (level.getRandom().nextDouble() > HfmrnvConfig.ENTITY_SPREAD_CHANCE.get()) {

                List<Entity> targets = level.getEntities(entity, entity.getBoundingBox().inflate(HfmrnvConfig.ENTITY_SPREAD_DISTANCE.get()));
                for (var target : targets) {
                    infect(host, level, target, HfmrnvConfig.ENTITY_SPREAD_EXPOSURES);
                }

            }
        } else if (host instanceof LevelChunk chunk && chunk.getLevel() instanceof ServerLevel level) {
            if (level.getRandom().nextDouble() >= HfmrnvConfig.ENTITY_SPREAD_CHANCE.get()) {
                level.entityManager.sectionStorage.getExistingSectionPositionsInChunk(chunk.getPos().toLong())
                        .mapToObj(level.entityManager.sectionStorage::getSection)
                        .filter(Objects::nonNull)
                        .flatMap(EntitySection::getEntities)
                        .forEach(target -> infect(host, level, target, HfmrnvConfig.ENTITY_SPREAD_EXPOSURES));
            }
            if (level.getRandom().nextDouble() > HfmrnvConfig.CHUNK_SPREAD_CHANCE.get()) {
                Direction direction = Direction.values()[level.getRandom().nextInt(2, 6)];
                ChunkPos pos = chunk.getPos();
                LevelChunk target = level.getChunk(pos.x + direction.getStepX(), pos.z + direction.getStepZ());

                infect(chunk, level, target, HfmrnvConfig.CHUNK_SPREAD_EXPOSURES);
            }
        }
    }

    private static void infect(IAttachmentHolder host, Level level, IAttachmentHolder target, IntSupplier maxExposures) {
        Optional<NanobotSwarm> targetSwarm = target.getExistingData(SWARM);
        if (targetSwarm.isPresent() && targetSwarm.get().hasEffect(PROTECTS_AGAINST_SPREAD)) return;

        final NanobotEffect effect;
        if (targetSwarm.isPresent())
            effect = host.getData(HmrnvRegistries.SWARM).randomEffectExcept(level.getRandom(), targetSwarm.get());
        else
            effect = host.getData(SWARM).randomEffect(level.getRandom());

        if (effect == null) return;

        ResourceLocation key = level.registryAccess().registryOrThrow(HmrnvRegistries.EFFECT.key()).getKey(effect);
        Object2IntMap<ResourceLocation> infections = target.getData(INFECTION);
        int exposures = infections.mergeInt(key, 1, Integer::sum);
        if (exposures >= maxExposures.getAsInt()) {
            NanobotSwarm.mergeSwarm(target, Object2IntMaps.singleton(effect, 1));
            infections.removeInt(effect);
            target.setData(INFECTION, infections);
        }
    }

    @Override
    public int getRequiredPower(int level) {
        return level * 4;
    }
}
