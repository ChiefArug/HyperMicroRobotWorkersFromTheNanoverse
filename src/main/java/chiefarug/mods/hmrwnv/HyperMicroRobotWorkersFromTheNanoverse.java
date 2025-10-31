package chiefarug.mods.hmrwnv;

import chiefarug.mods.hmrwnv.core.EffectConfiguration;
import chiefarug.mods.hmrwnv.core.NanobotSwarm;
import chiefarug.mods.hmrwnv.core.effect.HungerEffect;
import chiefarug.mods.hmrwnv.recipe.NanobotAddEffectRecipe;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static chiefarug.mods.hmrwnv.HfmrnvConfig.CHUNK_SLOW_DOWN_FACTOR;
import static chiefarug.mods.hmrwnv.HfmrnvConfig.ENTITY_SLOW_DOWN_FACTOR;
import static chiefarug.mods.hmrwnv.HfmrnvConfig.PLAYER_SLOW_DOWN_FACTOR;
import static chiefarug.mods.hmrwnv.HmrnvRegistries.EFFECTS_KEY;
import static chiefarug.mods.hmrwnv.HmrnvRegistries.INFECTION;
import static chiefarug.mods.hmrwnv.HmrnvRegistries.SWARM;
import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODID;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

// This class is basically unused cause who can be bothered typing such a long name.
@Mod(MODID)
@EventBusSubscriber(modid = MODID)
public class HyperMicroRobotWorkersFromTheNanoverse {
    public static final String MODID = "hmrw_nanoverse";
    public static final ResourceLocation MODRL = ResourceLocation.fromNamespaceAndPath(MODID, MODID);
    public static final Codec<ResourceLocation> MODRL_CODEC = Codec.STRING
            .xmap(location -> {
                int index = location.indexOf(':');
                if (index == -1) return MODRL.withPath(location);
                return ResourceLocation.fromNamespaceAndPath(location.substring(0, index), location.substring(index + 1));
            }, ResourceLocation::toString)
            .stable();
    public static final Logger LGGR = LogUtils.getLogger();
    @NotNull // Not null when it matters, ie after the server starts.
    @SuppressWarnings({"NullableProblems", "DataFlowIssue"})
    private static Integer seedHash = null;

    public HyperMicroRobotWorkersFromTheNanoverse(IEventBus modBus, ModContainer modContainer) {
        HmrnvRegistries.init(modBus);
        HungerEffect.init(modBus);
        NanobotAddEffectRecipe.init(modBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, HfmrnvConfig.SPEC);
    }

    @SubscribeEvent //TODO: translations
    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                literal("hmrwnv")
                        .then(literal("swarm")
                                .executes(c -> {
                                    CommandSourceStack source = c.getSource();
                                    Vec3 pos = source.getPosition();
                                    ChunkAccess chunk = source.getLevel().getChunk(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));
                                    source.sendSuccess(() -> Component.literal(chunk.getExistingData(SWARM).map(d -> "has data: " + d).orElse("no data")), true);
                                    return 1;
                                })
                                .then(literal("clear")
                                        .executes(c -> {
                                            CommandSourceStack source = c.getSource();
                                            Vec3 pos = source.getPosition();
                                            ChunkAccess chunk = source.getLevel().getChunk(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));
                                            if (chunk.hasData(SWARM)) {
                                                chunk.removeData(SWARM);
                                                source.sendSuccess(() -> Component.literal("cleared"), true);
                                                return 1;
                                            } else {
                                                source.sendFailure(Component.literal("no data"));
                                                return 0;
                                            }
                                        })))
                        .then(literal("infection")
                                .executes(c -> {
                                    CommandSourceStack source = c.getSource();
                                    Vec3 pos = source.getPosition();
                                    ChunkAccess chunk = source.getLevel().getChunk(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));
                                    source.sendSuccess(() -> Component.literal(chunk.getExistingData(INFECTION).map(d -> "has data: " + d).orElse("no data")), true);
                                    return 1;
                                })
                        )
                        .then(literal("add")
                                .then(argument("effect", ResourceKeyArgument.key(EFFECTS_KEY)).suggests(HyperMicroRobotWorkersFromTheNanoverse::suggestEffects)
                                        .then(argument("level", IntegerArgumentType.integer(0))
                                        .executes(c -> {
                                            CommandSourceStack source = c.getSource();
                                            Vec3 pos = source.getPosition();
                                            ChunkAccess chunk = source.getLevel().getChunk(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));
                                            ResourceKey effectKey = c.getArgument("effect", ResourceKey.class);
                                            EffectConfiguration<?> effect = source.registryAccess().registryOrThrow(EFFECTS_KEY).get(effectKey);
                                            int level = c.getArgument("level", Integer.class);
                                            NanobotSwarm.mergeSwarm(chunk, Object2IntMaps.singleton(effect, level));
                                            source.sendSuccess(() -> Component.literal("swarmed " + chunk + " with " + effectKey.location()), true);
                                            return 1;
                                        })))));
    }

    @SubscribeEvent
    private static void onServerStart(ServerAboutToStartEvent event) {
        seedHash = Long.hashCode(event.getServer().getWorldData().worldGenOptions().seed());
    }

    @SubscribeEvent
    private static void onChunkLoad(ChunkEvent.Load event) {
        if (event.isNewChunk()) {
            ChunkPos pos = event.getChunk().getPos();
            int chance = HfmrnvConfig.SWARM_GENERATION_CHANCE.getAsInt();
            int hash = 31 * seedHash + pos.hashCode();
            // chance to become swarmed is based on hash of world seed and hash of the position
            if (hash % chance == 0) {
                NanobotSwarm.attachSwarm(event.getChunk(), generateSpawnSwarmEffects(event.getLevel().registryAccess(), RandomSource.create(pos.toLong())));
                LGGR.debug("Swarmed chunk {}, {}", pos.x, pos.z);
            }
        }
    }

    @SubscribeEvent
    private static void tickEntitySwarms(EntityTickEvent.Pre event) {
        Entity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel sl) || !e.hasData(SWARM) || !(e instanceof LivingEntity)) return;
        int slowDown = ENTITY_SLOW_DOWN_FACTOR.getAsInt();
        int tick = (int) (sl.getGameTime() % slowDown);
        // evenly distribute ticks based on uuid
        if (tick == Math.abs(e.getUUID().getLeastSignificantBits()) % slowDown)
            e.getData(SWARM).tick(e);
    }

    @SubscribeEvent // entity tick event does not do players
    private static void tickPlayerSwarms(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !event.getEntity().hasData(SWARM)) return;
        int slowDown = PLAYER_SLOW_DOWN_FACTOR.getAsInt();
        int tick = (int) (sp.serverLevel().getGameTime() % slowDown);
        // evenly distribute ticks based on uuid
        if (tick ==  Math.abs(sp.getUUID().getLeastSignificantBits()) % slowDown)
            sp.getData(SWARM).tick(sp);
    }

    @SubscribeEvent
    private static void tickChunkSwarms(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            int slowDown = CHUNK_SLOW_DOWN_FACTOR.getAsInt();
            int tick = (int) (event.getLevel().getGameTime() % slowDown);

            ObjectBidirectionalIterator<Long2ObjectMap.Entry<ChunkHolder>> iter = sl.getChunkSource().chunkMap.visibleChunkMap.long2ObjectEntrySet().fastIterator();
            while (iter.hasNext()) {
                Long2ObjectMap.Entry<ChunkHolder> next = iter.next();

                // evenly distribute ticks based on position
                long posAsLong = next.getLongKey();
                if (tick == Math.abs(posAsLong % slowDown)) {
                    LevelChunk chunk = next.getValue().getTickingChunk();
                    if (chunk != null) {
                        NanobotSwarm swarm = chunk.getExistingDataOrNull(SWARM);
                        if (swarm != null) {
                            swarm.tick(chunk);

                            // tick entities inside the chunk
                            sl.entityManager.sectionStorage.getExistingSectionsInChunk(posAsLong)
                                    // use storage.find as that caches the results of the search for next time.
                                    .flatMap(entityEntitySection -> entityEntitySection.storage.find(LivingEntity.class).stream())
                                    .forEach(entity -> swarm.forEachEffect(entity, EffectConfiguration::onTick));
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent // trigger add/remove effects when entities enter and leave chunks
    private static void chunkSwarmAffectEntities(EntityEvent.EnteringSection event) {
        // don't trigger on only y level changes
        if (!event.didChunkChange()) return;
        // don't trigger on client side
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        // don't trigger on non living entities
        if (!(event.getEntity() instanceof LivingEntity)) return;

        SectionPos oldPos = event.getOldPos();
        Optional<Object2IntMap<EffectConfiguration<?>>> oldChunk = level.getChunk(oldPos.x(), oldPos.z())
                .getExistingData(SWARM)
                .map(NanobotSwarm::getEffects);

        SectionPos newPos = event.getNewPos();
        Optional<Object2IntMap<EffectConfiguration<?>>> newChunk = level.getChunk(newPos.x(), newPos.z())
                .getExistingData(SWARM)
                .map(NanobotSwarm::getEffects);

        Object2IntMap<EffectConfiguration<?>> toAdd = Object2IntMaps.emptyMap();;
        Object2IntMap<EffectConfiguration<?>> toRemove = Object2IntMaps.emptyMap();
        if (oldChunk.isPresent()) {
            Object2IntMap<EffectConfiguration<?>> oldSwarm = oldChunk.get();
            if (newChunk.isPresent()) {
                // both have data, find differences
                Object2IntMap<EffectConfiguration<?>> newSwarm = newChunk.get();
                toAdd = new Object2IntArrayMap<>(0);
                toRemove = new Object2IntArrayMap<>(0);

                for (EffectConfiguration<?> effect : collectEffects(oldSwarm, newSwarm)) {
                    // negative level values are not possible, so use this rando negative value.
                    final int NULL = -0xDEAD;
                    int newValue = newSwarm.getOrDefault(effect, NULL);
                    int oldValue = oldSwarm.getOrDefault(effect, NULL);

                    // if they are the same we don't need to care about them.
                    if (newValue == oldValue) continue;

                    if (newValue == NULL) { // oldValue is not null
                        toRemove.put(effect, oldValue);
                    } else if (oldValue == NULL) { // newValue is not null
                        toAdd.put(effect, newValue);
                    } else { // both are not null and also not equal
                        toRemove.put(effect, oldValue);
                        toAdd.put(effect, newValue);
                    }
                }
            } else {
                // only old has data, remove everything
                toRemove = oldSwarm;
            }
        } else if (newChunk.isPresent()) {
            // only new has data, add everything
            toAdd = newChunk.get();
        }

        Entity entity = event.getEntity();
        NanobotSwarm.forEachEffect(toRemove, entity, EffectConfiguration::onRemove);
        NanobotSwarm.forEachEffect(toAdd, entity, EffectConfiguration::onAdd);
    }

    private static Set<EffectConfiguration<?>> collectEffects(Object2IntMap<EffectConfiguration<?>> oldSwarm, Object2IntMap<EffectConfiguration<?>> newSwarm) {
        Set<EffectConfiguration<?>> effects = new HashSet<>(oldSwarm.size() + newSwarm.size());
        for (EffectConfiguration<?> effectConfiguration : oldSwarm.keySet()) {
            if (effectConfiguration.affectsEntitiesInChunk())
                effects.add(effectConfiguration);
        }
        for (EffectConfiguration<?> effectConfiguration : newSwarm.keySet()) {
            if (effectConfiguration.affectsEntitiesInChunk())
                effects.add(effectConfiguration);
        }
        return effects;
    }

    // TODO:?make recording snippets of the end poem that play in nanobot clouds
    //TODO: infect on spawn entities in an entity tag (also those who aren't should still have the chunks effects added temporarily)
    //TODO: max level of effects
    //TODO: unhardcode this
    private static Map<EffectConfiguration<?>, Integer> generateSpawnSwarmEffects(RegistryAccess access, RandomSource random) {
        Registry<EffectConfiguration<?>> reg = access.registryOrThrow(EFFECTS_KEY);
        return Map.of(
                random.nextDouble() > 0.8 ?
                        reg.get(MODRL.withPath("ravenous")) :
                        reg.get(MODRL.withPath("hunger")),
                4,
                reg.get(MODRL.withPath("spread")), 8, reg.get(MODRL.withPath("attribute/bigger")), 5);
    }

    private static CompletableFuture<Suggestions> suggestEffects(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(ctx.getSource().registryAccess().registryOrThrow(EFFECTS_KEY).keySet(), builder);
    }
}
