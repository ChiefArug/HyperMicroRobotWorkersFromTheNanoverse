package chiefarug.mods.hmrwnv.item;

import chiefarug.mods.hmrwnv.core.EffectConfiguration;
import chiefarug.mods.hmrwnv.core.NanobotSwarm;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.SWARM;
import static net.minecraft.SharedConstants.TICKS_PER_SECOND;

public class NanobotItem extends Item {
    public NanobotItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return stack.has(SWARM) ? 5 * TICKS_PER_SECOND : super.getUseDuration(stack, entity);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        Object2IntMap<EffectConfiguration<?>> swarm = getSwarm(stack);
        if (swarm == null) return super.finishUsingItem(stack, level, entity);

        NanobotSwarm.attachSwarm(entity, swarm);

        stack.shrink(1);
        return stack;
    }
//
//    @Override
//    public InteractionResult useOn(UseOnContext context) {
//        Player player = context.getPlayer();
//        if (player == null)
//            return super.useOn(context);
//
//        return swarm(player, context.getLevel().getChunk(context.getClickedPos().getX(), context.getClickedPos().getY()), context.getItemInHand());
//    }
//
//    @Override
//    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
//        return swarm(player, interactionTarget, stack);


    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        Level level = context.level();
        Object2IntMap<EffectConfiguration<?>> effects = getSwarm(stack);
        if (effects == null) return;
        if (level != null) {
            tooltipComponents.addAll(effects.object2IntEntrySet().stream()
                    .map(e -> e.getKey().nameWithLevel(level.registryAccess(), e.getIntValue()).withStyle(ChatFormatting.GRAY))
                    .toList());
        } else if (!effects.isEmpty()){
            tooltipComponents.add(Component.translatable("hmrw_nanoverse.tooltip.cannot_display_effects"));
        }

    }

    @Nullable
    @Unmodifiable
    public static Object2IntMap<EffectConfiguration<?>> getSwarm(ItemStack stack) {
        return stack.get(SWARM);
    }

}
