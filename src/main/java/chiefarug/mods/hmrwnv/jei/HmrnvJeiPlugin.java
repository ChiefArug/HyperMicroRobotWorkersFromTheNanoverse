package chiefarug.mods.hmrwnv.jei;

import chiefarug.mods.hmrwnv.HmrnvRegistries;
import chiefarug.mods.hmrwnv.core.effect.NanobotEffect;
import chiefarug.mods.hmrwnv.recipe.NanobotAddEffectRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IModInfoRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODID;
import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODRL;

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
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new NanobotEffectInfo(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IIngredientManager ingredients = registration.getIngredientManager();
        Map<NanobotEffect, List<ITypedIngredient<ItemStack>>> effectsToIngredients = new HashMap<>();
        for (ItemStack itemStack : registration.getJeiHelpers().getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK)) {
            NanobotEffect effect = NanobotAddEffectRecipe.getEffect(itemStack.getItem());
            if (effect != null) {
                //noinspection OptionalGetWithoutIsPresent // This is safe as we get it from a list of all ingredients
                effectsToIngredients.computeIfAbsent(effect, (k) -> new ArrayList<>())
                        .add(ingredients.createTypedIngredient(VanillaTypes.ITEM_STACK, itemStack).get());
            }
        }

        List<NanobotEffectInfo.InfoRecipe> list = new ArrayList<>(effectsToIngredients.size());
        for (Map.Entry<NanobotEffect, List<ITypedIngredient<ItemStack>>> entry : effectsToIngredients.entrySet()) {
            ResourceLocation key = Objects.requireNonNull(HmrnvRegistries.EFFECT.getKey(entry.getKey()));
            List<FormattedText> description = List.of(
                    Component.translatable(key.toLanguageKey(HmrnvRegistries.EFFECT.key().location().getPath())).withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE),
                    Component.translatable(key.toLanguageKey(HmrnvRegistries.EFFECT.key().location().getPath()) + ".description")
            );
            list.add(new NanobotEffectInfo.InfoRecipe(entry.getValue(), description));
        };
        registration.addRecipes(NanobotEffectInfo.TYPE, list);
        registration.addRecipes(RecipeTypes.CRAFTING, List.of());
    }
}
