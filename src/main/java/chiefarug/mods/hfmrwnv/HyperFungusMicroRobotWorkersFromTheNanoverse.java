package chiefarug.mods.hfmrwnv;

import chiefarug.mods.hfmrwnv.core.NanobotSwarm;
import chiefarug.mods.hfmrwnv.core.effect.HungerEffect;
import chiefarug.mods.hfmrwnv.core.effect.NanobotEffect;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
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
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Map;

import static chiefarug.mods.hfmrwnv.HfmrnvConfig.CHUNK_SLOW_DOWN_FACTOR;
import static chiefarug.mods.hfmrwnv.HfmrnvConfig.ENTITY_SLOW_DOWN_FACTOR;
import static chiefarug.mods.hfmrwnv.HfmrnvConfig.PLAYER_SLOW_DOWN_FACTOR;
import static chiefarug.mods.hfmrwnv.HfmrnvRegistries.SWARM;
import static chiefarug.mods.hfmrwnv.HyperFungusMicroRobotWorkersFromTheNanoverse.MODID;
import static net.minecraft.commands.Commands.literal;

// This class is basically unused cause who can be bothered typing such a long name.
@Mod(MODID)
@EventBusSubscriber(modid = MODID)
public class HyperFungusMicroRobotWorkersFromTheNanoverse {
    public static final String MODID = "hfmrw_nanoverse";
    public static final ResourceLocation MODRL = ResourceLocation.fromNamespaceAndPath(MODID, MODID);
    public static final Logger LGGR = LogUtils.getLogger();
    @NotNull // Not null when it matters, ie after the server starts.
    @SuppressWarnings({"NullableProblems", "DataFlowIssue"})
    private static Integer seedHash = null;

    public HyperFungusMicroRobotWorkersFromTheNanoverse(IEventBus modBus, ModContainer modContainer) {
        HfmrnvRegistries.init(modBus);
        HungerEffect.init(modBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, HfmrnvConfig.SPEC);
    }

    @SubscribeEvent
    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                literal("hfmrwnv")
                        .executes(c -> {
                            CommandSourceStack source = c.getSource();
                            Vec3 pos = source.getPosition();
                            ChunkAccess chunk = source.getLevel().getChunk(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));
                            source.sendSuccess(() -> Component.literal(chunk.hasData(SWARM) ? "has data: " + chunk.getData(SWARM) : "no data"), true);
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
                                })
        ));
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
                NanobotSwarm.attachSwarm(event.getChunk(), generateSpawnSwarmEffects(RandomSource.create(pos.toLong())));
                LGGR.debug("Swarmed chunk {}, {}", pos.x, pos.z);
            }
        }
    }

    @SubscribeEvent
    private static void tickEntitySwarms(EntityTickEvent.Pre event) {
        Entity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel sl) || !e.hasData(SWARM)) return;
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
                if (tick == Math.abs(next.getLongKey() % slowDown)) {
                    LevelChunk tickingChunk = next.getValue().getTickingChunk();
                    if (tickingChunk != null) {
                        NanobotSwarm swarm = tickingChunk.getExistingDataOrNull(SWARM);
                        if (swarm != null) {
                            swarm.tick(tickingChunk);
                        }
                    }
                }
            }
        }
    }


    private static Map<NanobotEffect, Integer> generateSpawnSwarmEffects(RandomSource random) {
        return Map.of(
                random.nextDouble() > 0.8 ?
                        HfmrnvRegistries.RAVENOUS.get() :
                        HfmrnvRegistries.HUNGER.get(),
                4,
                HfmrnvRegistries.SPREAD.get(), 8);
    }

}
