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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODRL;

/// Clone of JEI's built in info category that holds info on the effects of materials used to make nanobots
public class NanobotEffectInfo extends AbstractRecipeCategory<NanobotEffectInfo.InfoRecipe> {
    private static final int recipeWidth = 170;
    private static final int recipeHeight = 125;
    private static final int recipeYPos = 22;
    public static final RecipeType<InfoRecipe> TYPE = new RecipeType<>(MODRL.withPath("effect_information"), InfoRecipe.class);

    public NanobotEffectInfo(IGuiHelper helper) {
        super(TYPE, Component.translatable("gui.hmrw_nanoverse.category.effectInformation"),
                helper.createDrawableItemLike(HmrnvRegistries.NANOBOTS),
                recipeWidth, recipeHeight);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, InfoRecipe recipe, IFocusGroup focuses) {
        int xPos = (recipeWidth - 16) / 2;

        IRecipeSlotBuilder inputSlotBuilder = builder.addInputSlot(xPos, 1)
                .setStandardSlotBackground();

        IIngredientAcceptor<?> outputSlotBuilder = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);

        for (ITypedIngredient<?> typedIngredient : recipe.ingredients()) {
            inputSlotBuilder.addTypedIngredient(typedIngredient);
            outputSlotBuilder.addTypedIngredient(typedIngredient);
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, InfoRecipe recipe, IFocusGroup focuses) {
        int height = recipeHeight - recipeYPos;
        builder.addScrollBoxWidget(recipeWidth, height, 0, recipeYPos)
                .setContents(recipe.info());
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(InfoRecipe recipe) {
        return null;
    }

    public record InfoRecipe(List<ITypedIngredient<ItemStack>> ingredients, List<FormattedText> info) {}
}
