package chiefarug.mods.hmrwnv;

import chiefarug.mods.hmrwnv.core.NanobotSwarm;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.BOT_VISION;
import static chiefarug.mods.hmrwnv.HmrnvRegistries.BOT_VISION_EFFECT;
import static chiefarug.mods.hmrwnv.HmrnvRegistries.BOT_VISION_ITEM;
import static chiefarug.mods.hmrwnv.HmrnvRegistries.INFECTION;
import static chiefarug.mods.hmrwnv.HmrnvRegistries.SWARM;
import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODID;
import static net.minecraft.client.renderer.blockentity.BeaconRenderer.BEAM_LOCATION;
import static net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES;

@Mod(value = MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class HfmrnvClient {
    public HfmrnvClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    private static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != AFTER_BLOCK_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        Optional<NanobotSwarm> swarm = mc.player.getExistingData(SWARM);
        if (!( // three ways to get bot vision
                helmet.is(BOT_VISION_ITEM) ||
                helmet.canPerformAction(BOT_VISION) ||
                swarm.isPresent() && swarm.get().hasEffect(BOT_VISION_EFFECT)
        )) return;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        {
            Vec3 projectedView = event.getCamera().getPosition();
            pose.translate(-projectedView.x, -projectedView.y, -projectedView.z);

            var bufferSource = mc.renderBuffers().bufferSource();
            var partialTick  = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            var level        = Objects.requireNonNull(mc.level);


            AtomicReferenceArray<LevelChunk> iter = level.getChunkSource().storage.chunks;
            for (int i = 0; i < iter.length(); i++) {
                LevelChunk next = iter.get(i);
                if (next == null) continue;
                next.getExistingData(SWARM);
                if (next.hasData(SWARM)) {
                    float baseX = next.getPos().getMiddleBlockX() + 0.5f;
                    float baseZ = next.getPos().getMiddleBlockZ() + 0.5f;
                    renderBeam(pose, baseX, level.getMinBuildHeight(), baseZ, level.getHeight(), bufferSource, partialTick, level, DyeColor.CYAN.getTextureDiffuseColor());
                }
            }

            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof LivingEntity)) continue;
                if (entity.hasData(SWARM)) {
                    Vec3 pos = entity.getPosition(partialTick);
                    renderBeam(pose, pos.x, pos.y, pos.z, 3, bufferSource, partialTick, level, DyeColor.RED.getTextureDiffuseColor());

                } else if (entity.hasData(INFECTION)) {
                    Vec3 pos = entity.getPosition(partialTick);
                    renderBeam(pose, pos.x, pos.y, pos.z, 3, bufferSource, partialTick, level, DyeColor.BROWN.getTextureDiffuseColor());
                }
            }

        }
        pose.popPose();

    }

    // TODO: we probably want something slightly more interesting that a beacon beam
    private static void renderBeam(PoseStack pose, double x, double y, double z, int height, MultiBufferSource.BufferSource bufferSource, float partialTick, ClientLevel level, int color) {
        pose.pushPose();
        {
            pose.translate(x, y, z);
            BeaconRenderer.renderBeaconBeam(pose, bufferSource, BEAM_LOCATION, partialTick, 1, level.getGameTime(), 0, height, color, 0.2F, 0.25F);
        }
        pose.popPose();
    }

    /// This is required as neo gives you server side datamap objects in singleplayer even if querying from a client side reg access
    /// (I suspect this is because both sides will use the same item registry object and as such it has no context as to which side it is on)
    /// so we just use the server side reg access if on singleplayer client
    // TODO: bug report this
    public static RegistryAccess getAuthoritiveRegistryAccess() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer()) {
            // we are singleplayer, so use the server
            return Objects.requireNonNull(mc.getSingleplayerServer()).registryAccess();
        } else {
            // we are multiplayer so we only have the client values and don't need to care about differences
            return Objects.requireNonNull(mc.level).registryAccess();
        }
    }
}
