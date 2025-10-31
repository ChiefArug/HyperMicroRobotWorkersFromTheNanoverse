package chiefarug.mods.hmrwnv.recipe;

import chiefarug.mods.hmrwnv.HmrnvRegistries;
import chiefarug.mods.hmrwnv.core.EffectConfiguration;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.SWARM;
import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODRL;

public class NanobotAddEffectRecipe extends CustomRecipe implements RecipeSerializer<NanobotAddEffectRecipe> {
    public static final NanobotAddEffectRecipe INSTANCE = new NanobotAddEffectRecipe();
    public static final MapCodec<NanobotAddEffectRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, NanobotAddEffectRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private static final DataMapType<Item, EffectConfiguration<?>> ITEM_EFFECTS = DataMapType.builder(
            MODRL.withPath("effects"),
            Registries.ITEM,
            EffectConfiguration.BY_ID_CODEC
    ).synced(EffectConfiguration.CLIENT_CODEC, true).build();



    @Nullable
    public static EffectConfiguration<?> getEffect(ItemLike item) {
        return BuiltInRegistries.ITEM.wrapAsHolder(item.asItem()).getData(ITEM_EFFECTS);
    }

    public NanobotAddEffectRecipe() {
        super(CraftingBookCategory.MISC);
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterDataMapTypesEvent event) -> event.register(ITEM_EFFECTS));
    }

    public static boolean hasEffect(ItemStack item) {
        return hasEffect(item.getItem());
    }

    public static boolean hasEffect(ItemLike item) {
        return getEffect(item) != null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean bot = false;
        int effects = 0;
        for (int i = 0; i < input.size(); i++) {
            var in = input.getItem(i);
            if (in.isEmpty()) continue;

            if (in.is(HmrnvRegistries.NANOBOTS)) {
                // if we have seen a bot swarm before we can't match
                if (!(bot = !bot)) return false;
            } else if (!hasEffect(in.getItem())) {
                return false;
            } else {
                effects++;
            }
        }
        // TODO: this needs to calculate if any are above max level!
        return effects > 0;
    }


    public static ItemStack assemble(int size, IntFunction<ItemStack> items) {
        // use an array map because it's going to be tiny and fast iteration will be useful when it gets turned into a swarm
        // capacity of size - 1 as we know the bot won't have an effect
        Object2IntArrayMap<EffectConfiguration<?>> effects = new Object2IntArrayMap<>(size - 1);
        ItemStack bots = null;
        for (int i = 0; i < size; i++) {
            ItemStack item = items.apply(i);
            if (item.is(HmrnvRegistries.NANOBOTS)) {
                bots = item;
                continue;
            }
            EffectConfiguration<?> effect = getEffect(item.getItem());
            if (effect != null)
                effects.mergeInt(effect, 1, Integer::sum);
        }

        if (bots == null) return ItemStack.EMPTY; // this should be impossible but in testing I found it was not...

        ItemStack stack = bots.copyWithCount(1);
        stack.set(SWARM, Object2IntMaps.unmodifiable(effects));
        return stack;
    }


    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return assemble(input.size(), input::getItem);
    }


    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height > 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return this;
    }

    @Override
    public MapCodec<NanobotAddEffectRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, NanobotAddEffectRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
