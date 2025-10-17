package chiefarug.mods.hfmrwnv;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.world.item.DyeColor;
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
import java.util.concurrent.atomic.AtomicReferenceArray;

import static chiefarug.mods.hfmrwnv.HfmrnvRegistries.SWARM;
import static chiefarug.mods.hfmrwnv.HyperFungusMicroRobotWorkersFromTheNanoVerse.MODID;
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
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        {
            Vec3 projectedView  = event.getCamera().getPosition();
            pose.translate(-projectedView.x, -projectedView.y, -projectedView.z);

            var bufferSource    = Minecraft.getInstance().renderBuffers().bufferSource();
            var partialTick     = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            var color           = DyeColor.CYAN.getTextureDiffuseColor();
            var level           = Objects.requireNonNull(Minecraft.getInstance().level);


            AtomicReferenceArray<LevelChunk> iter = level.getChunkSource().storage.chunks;
            for (int i = 0; i < iter.length(); i++) {
                LevelChunk next = iter.get(i);
                if (next == null) continue;
                if (next.hasData(SWARM.attachment())) {
                    float baseX = next.getPos().getMiddleBlockX() + 0.5f;
                    float baseZ = next.getPos().getMiddleBlockZ() + 0.5f;
                    pose.pushPose();
                    pose.translate(baseX, 0, baseZ);
                    BeaconRenderer.renderBeaconBeam(pose, bufferSource, BEAM_LOCATION, partialTick, 1, level.getGameTime(), 0, 500, color, 0.2F, 0.25F);;
                    pose.popPose();
                }
            }



        }
        pose.popPose();


    }
}
