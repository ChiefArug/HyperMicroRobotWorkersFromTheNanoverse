package chiefarug.mods.hmrwnv.mixin;

import chiefarug.mods.hmrwnv.core.NanobotSwarm;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.function.Supplier;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.PREVENTS_RANDOM_TICKS;
import static chiefarug.mods.hmrwnv.HmrnvRegistries.SWARM;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {
    protected ServerLevelMixin(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, Supplier<ProfilerFiller> profiler, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, profiler, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Definition(id = "randomTickSpeed", local = @Local(type = int.class, argsOnly = true))
    @Expression("randomTickSpeed > 0")
    @ModifyExpressionValue(method = "tickChunk", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean hmrw_nanoverse$consumedChunksDoNotRandomTick(boolean original, LevelChunk chunk) {
        Optional<NanobotSwarm> swarm = chunk.getExistingData(SWARM);
        if (swarm.isPresent() && swarm.get().hasEffect(registryAccess(), PREVENTS_RANDOM_TICKS)) return false;
        return original;
    }

}
