package chiefarug.mods.hmrwnv.jei;

import chiefarug.mods.hmrwnv.HmrnvRegistries;
import chiefarug.mods.hmrwnv.core.EffectConfiguration;
import chiefarug.mods.hmrwnv.recipe.NanobotAddEffectRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import mezz.jei.api.registration.IModInfoRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.util.ConcatenatedListView;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODID;
import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODRL;
import static chiefarug.mods.hmrwnv.core.EffectConfiguration.description;
import static chiefarug.mods.hmrwnv.core.EffectConfiguration.name;

@JeiPlugin
public class HmrnvJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return MODRL;
    }

    @Override
    public void registerModInfo(IModInfoRegistration modAliasRegistration) {
        modAliasRegistration.addModAliases(MODID, "HMRNV", "HMR Nanoverse", "HMR from the Nanoverse" /*Full Mod name is included by default*/);
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        // ap[p
        for (DeferredHolder<Item, ? extends Item> item : HmrnvRegistries.getAllItems()) {
            String acronym = item.get().getDescriptionId() + ".acronym";
            if (Language.getInstance().has(acronym)) {
                registration.addAlias(VanillaTypes.ITEM_STACK, item.get().getDefaultInstance(), acronym);
            }
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new NanobotEffectInfo(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(HmrnvRegistries.NANOBOTS, NanobotEffectInfo.TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IIngredientManager ingredients = registration.getIngredientManager();
        Map<Holder<EffectConfiguration<?>>, List<ITypedIngredient<ItemStack>>> effectsToIngredients = new HashMap<>();
        for (ItemStack itemStack : registration.getJeiHelpers().getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK)) {
            Holder<EffectConfiguration<?>> effect = NanobotAddEffectRecipe.getEffect(itemStack.getItem());
            if (effect != null) {
                //noinspection OptionalGetWithoutIsPresent // This is safe as we get it from a list of all ingredients
                effectsToIngredients.computeIfAbsent(effect, k -> new ArrayList<>())
                        .add(ingredients.createTypedIngredient(VanillaTypes.ITEM_STACK, itemStack).get());
            }
        }


        List<NanobotEffectInfo.InfoRecipe> list = new ArrayList<>(effectsToIngredients.size());
        for (Map.Entry<Holder<EffectConfiguration<?>>, List<ITypedIngredient<ItemStack>>> entry : effectsToIngredients.entrySet()) {
            list.add(new NanobotEffectInfo.InfoRecipe(entry.getValue(), List.of(
                    name(entry.getKey()).withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE),
                    description(entry.getKey())
            )));
        };
        registration.addRecipes(NanobotEffectInfo.TYPE, list);
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        List<ItemStack> allEffects = new ArrayList<>();
        for (ItemStack ingredient : registration.getJeiHelpers().getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK)) {
            Holder<EffectConfiguration<?>> effect = NanobotAddEffectRecipe.getEffect(ingredient.getItem());
            if (effect != null) allEffects.add(ingredient);
        }
        registration.getCraftingCategory().addExtension(NanobotAddEffectRecipe.class, new ICraftingCategoryExtension<>() {
            @Override
            public void setRecipe(RecipeHolder<NanobotAddEffectRecipe> recipeHolder, IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
                List<ItemStack> focusedEffects = focuses.isEmpty() ? List.of() : focuses.getItemStackFocuses()
                        .map(IFocus::getTypedValue)
                        .map(ITypedIngredient::getIngredient)
                        .filter(NanobotAddEffectRecipe::hasEffect)
                        // we have to set count to 1 as JEI does not normalise the stacks.
                        .map(itemStack -> itemStack.copyWithCount(1))
                        .toList();

                List<ItemStack> bot = List.of(HmrnvRegistries.NANOBOTS.get().getDefaultInstance());
                List<ItemStack> first;
                List<ItemStack> second;

                if (focusedEffects.isEmpty()) {
                    // no focused items, show everything
                    first = allEffects;
                    second = allEffects;
                } else {
                    // the first slot gets dedicated to focused items
                    // the second get dedicated to everything except the focused item (else JEI will only show that), plus air
                    first = focusedEffects;
                    second = new ArrayList<>(allEffects.size() - focusedEffects.size());
                    for (ItemStack stack : allEffects) {
                        if (focusedEffects.stream().map(ItemStack::getItem).noneMatch(stack.getItem()::equals))
                            second.add(stack);
                    }
                }
                List<ItemStack> empty = List.of(ItemStack.EMPTY);

                // the third has everything from the second but shuffled (so it displays in a different order).
                List<ItemStack> third = new ArrayList<>(second);
                Collections.shuffle(third);

                // the second and third have empty as well
                second = ConcatenatedListView.of(second, empty);
                third = ConcatenatedListView.of(empty, third);

                craftingGridHelper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK,
                        List.of(bot, first, second, third), 3, 1);
                craftingGridHelper.createAndSetOutputs(builder, List.of(HmrnvRegistries.NANOBOTS.get().getDefaultInstance()));
            }

            @Override
            public void onDisplayedIngredientsUpdate(RecipeHolder<NanobotAddEffectRecipe> recipeHolder, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses) {
                List<ItemStack> inputs = new ArrayList<>(recipeSlots.size() - 1);
                IRecipeSlotDrawable output = null;
                for (IRecipeSlotDrawable recipeSlot : recipeSlots) {
                    switch(recipeSlot.getRole()) {
                        case RecipeIngredientRole.INPUT -> {
                            Optional<ITypedIngredient<?>> displayedIngredient = recipeSlot.getDisplayedIngredient();
                            if (displayedIngredient.isPresent())
                                inputs.add((ItemStack) displayedIngredient.get().getIngredient());
                        }
                        case OUTPUT -> output = recipeSlot;
                    }
                }

                Objects.requireNonNull(output).createDisplayOverrides().addItemStack(NanobotAddEffectRecipe.assemble(inputs.size(), inputs::get));
            }
        });
    }
}
