package chiefarug.mods.hfmrwnv;

import chiefarug.mods.hfmrwnv.core.NanobotSwarm;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import static chiefarug.mods.hfmrwnv.HfmrnvConfig.CHUNK_SLOW_DOWN_FACTOR;
import static chiefarug.mods.hfmrwnv.HfmrnvConfig.ENTITY_SLOW_DOWN_FACTOR;
import static chiefarug.mods.hfmrwnv.HfmrnvConfig.PLAYER_SLOW_DOWN_FACTOR;
import static chiefarug.mods.hfmrwnv.HfmrnvRegistries.SWARM;
import static chiefarug.mods.hfmrwnv.HyperFungusMicroRobotWorkersFromTheNanoVerse.MODID;
import static net.minecraft.commands.Commands.literal;

// This class is basically unused cause who can be bothered typing such a long name.
@Mod(MODID)
@EventBusSubscriber(modid = MODID)
public class HyperFungusMicroRobotWorkersFromTheNanoVerse {
    public static final String MODID = "hfmrw_nanoverse";
    public static final ResourceLocation MODRL = ResourceLocation.fromNamespaceAndPath(MODID, MODID);
    public static final Logger LGGR = LogUtils.getLogger();
    @Nullable
    private static Integer seedHash = null;


    public HyperFungusMicroRobotWorkersFromTheNanoVerse(IEventBus modEventBus, ModContainer modContainer) {
        HfmrnvRegistries.init(modEventBus);
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
            int hash = Objects.requireNonNull(seedHash) ^ pos.hashCode();
            int chance = HfmrnvConfig.SWARM_GENERATION_CHANCE.getAsInt();
            if (hash % chance == 0) {
                event.getChunk().setData(SWARM, new NanobotSwarm(ResourceLocation.fromNamespaceAndPath(MODID, String.valueOf(pos.z) + pos.x), List.of()));
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
        if (tick == e.getUUID().getLeastSignificantBits() % slowDown)
            e.getData(SWARM).tickEntity(sl, e);
    }

    @SubscribeEvent // entity tick event does not do players
    private static void tickPlayerSwarms(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !event.getEntity().hasData(SWARM)) return;
        int slowDown = PLAYER_SLOW_DOWN_FACTOR.getAsInt();
        int tick = (int) (sp.serverLevel().getGameTime() % slowDown);
        // evenly distribute ticks based on uuid
        if (tick == sp.getUUID().getLeastSignificantBits() % slowDown)
            sp.getData(SWARM).tickPlayer(sp.serverLevel(), sp);
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
                if (tick == next.getLongKey() % slowDown) {
                    LevelChunk tickingChunk = next.getValue().getTickingChunk();
                    if (tickingChunk != null && tickingChunk.hasData(SWARM)) {
                        System.out.println("ticking " + next.getValue().getPos());
                        tickingChunk.getData(SWARM).tickChunk(sl, tickingChunk);
                    }
                }
            }
        }
    }
}
