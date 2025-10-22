package chiefarug.mods.hmrwnv.recipe;

import chiefarug.mods.hmrwnv.HmrnvRegistries;
import chiefarug.mods.hmrwnv.core.NanobotSwarm;
import chiefarug.mods.hmrwnv.core.effect.NanobotEffect;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.SWARM;
import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODRL;
import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODRL_CODEC;

public class NanobotAddEffectRecipe extends CustomRecipe {
    private static final DataMapType<Item, NanobotEffect> ITEM_EFFECTS = DataMapType.builder(
            MODRL.withPath("effects"),
            Registries.ITEM,
            MODRL_CODEC.xmap(HmrnvRegistries.EFFECT::get, HmrnvRegistries.EFFECT::getKey)
    ).build();
    public static final RecipeSerializer<NanobotAddEffectRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(NanobotAddEffectRecipe::new);

    @Nullable
    public static NanobotEffect getEffect(ItemLike item) {
        return BuiltInRegistries.ITEM.wrapAsHolder(item.asItem()).getData(ITEM_EFFECTS);
    }

    public NanobotAddEffectRecipe(CraftingBookCategory category) {
        super(category);
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterDataMapTypesEvent event) -> event.register(ITEM_EFFECTS));
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean bot = false;
        int effects = 0;
        for (int i = 0; i < input.size(); i++) {
            var in = input.getItem(i);
            if (in.is(HmrnvRegistries.NANOBOTS)) {
                // if we have seen a bot swarm before we can't match
                if (!(bot = !bot)) return false;
            } else if (getEffect(in.getItem()) == null) {
                return false;
            } else {
                effects++;
            }
        }
        return effects > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        // use an array map because it's going to be tiny and fast iteration will be useful when it gets turned into a swarm
        // capacity of size - 1
        Object2IntArrayMap<NanobotEffect> effects = new Object2IntArrayMap<>(input.size() - 1);
        ItemStack bots = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack item = input.getItem(i);
            if (item.is(HmrnvRegistries.NANOBOTS)) {
                bots = item;
                continue;
            }
            NanobotEffect effect = getEffect(item.getItem());
            if (effect != null)
                effects.mergeInt(effect, 1, Integer::sum);
        }

        if (bots == null) throw new IllegalStateException("this should be impossible - NanobotAddEffectRecipe#assemble called without nanobots in input");

        ItemStack stack = bots.copy();
        stack.setCount(1);
        stack.set(SWARM, NanobotSwarm.createLooseSwarm(effects));
        return stack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height > 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
