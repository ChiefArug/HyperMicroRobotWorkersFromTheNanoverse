package chiefarug.mods.hmrwnv.jei;

import chiefarug.mods.hmrwnv.HmrnvRegistries;
import chiefarug.mods.hmrwnv.core.effect.NanobotEffect;
import chiefarug.mods.hmrwnv.recipe.NanobotAddEffectRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.vanilla.IJeiIngredientInfoRecipe;
import mezz.jei.api.registration.IModInfoRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

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
        List<IJeiIngredientInfoRecipe> list = new ArrayList<>();
        for (ItemStack stack : registration.getJeiHelpers().getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK)) {
            NanobotEffect effect = NanobotAddEffectRecipe.getEffect(stack.getItem());
            if (effect != null) {
                list.add(new IJeiIngredientInfoRecipe() {
                    @SuppressWarnings("OptionalGetWithoutIsPresent") // safe
                    @Override
                    public @Unmodifiable List<ITypedIngredient<?>> getIngredients() {
                        return List.of(ingredients.createTypedIngredient(stack).get());
                    }

                    @SuppressWarnings("DataFlowIssue")
                    @Override
                    public @Unmodifiable List<FormattedText> getDescription() {
                        return List.of(Component.translatable(HmrnvRegistries.EFFECT.getKey(effect).toLanguageKey(HmrnvRegistries.EFFECT.key().location().getPath())));
                    }
                });
            }
        }
        registration.addRecipes(NanobotEffectInfo.TYPE, list);
        registration.addRecipes(RecipeTypes.CRAFTING, List.of());
    }
}
