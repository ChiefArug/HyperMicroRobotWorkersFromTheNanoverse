package chiefarug.mods.hmrwnv.jei;

import chiefarug.mods.hmrwnv.HmrnvRegistries;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.vanilla.IJeiIngredientInfoRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODRL;

/// Clone of JEI's built in info category that holds info on the effects of materials used to make nanobots
public class NanobotEffectInfo extends AbstractRecipeCategory<IJeiIngredientInfoRecipe> {
    private static final int recipeWidth = 170;
    private static final int recipeHeight = 125;
    private static final int recipeYPos = 22;
    public static final RecipeType<IJeiIngredientInfoRecipe> TYPE = new RecipeType<>(MODRL.withPath("effect_information"), IJeiIngredientInfoRecipe.class);

    public NanobotEffectInfo(IGuiHelper helper) {
        super(TYPE, Component.translatable("gui.hmrw_nanoverse.category.effectInformation"),
                helper.createDrawableItemLike(HmrnvRegistries.NANOBOTS),
                recipeWidth, recipeHeight);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, IJeiIngredientInfoRecipe recipe, IFocusGroup focuses) {
        int xPos = (recipeWidth - 16) / 2;

        IRecipeSlotBuilder inputSlotBuilder = builder.addInputSlot(xPos, 1)
                .setStandardSlotBackground();

        IIngredientAcceptor<?> outputSlotBuilder = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);

        for (ITypedIngredient<?> typedIngredient : recipe.getIngredients()) {
            inputSlotBuilder.addTypedIngredient(typedIngredient);
            outputSlotBuilder.addTypedIngredient(typedIngredient);
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, IJeiIngredientInfoRecipe recipe, IFocusGroup focuses) {
        int height = recipeHeight - recipeYPos;
        builder.addScrollBoxWidget(recipeWidth, height, 0, recipeYPos)
                .setContents(recipe.getDescription());
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(IJeiIngredientInfoRecipe recipe) {
        return null;
    }
}
