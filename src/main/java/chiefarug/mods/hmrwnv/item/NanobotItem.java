package chiefarug.mods.hmrwnv.item;

import chiefarug.mods.hmrwnv.core.NanobotSwarm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import static chiefarug.mods.hmrwnv.HfmrnvRegistries.SWARM;
import static net.minecraft.SharedConstants.TICKS_PER_SECOND;

public class NanobotItem extends Item {
    public NanobotItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 5 * TICKS_PER_SECOND;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        NanobotSwarm swarm = getSwarm(stack);
        if (swarm == null) return stack;

        entity.setData(SWARM, swarm);

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

    @Nullable
    public static NanobotSwarm getSwarm(ItemStack stack) {
        return stack.get(SWARM);
    }

}
