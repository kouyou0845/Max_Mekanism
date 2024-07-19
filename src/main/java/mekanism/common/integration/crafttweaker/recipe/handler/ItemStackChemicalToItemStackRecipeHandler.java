package mekanism.common.integration.crafttweaker.recipe.handler;

import com.blamejared.crafttweaker.api.recipe.handler.IRecipeHandler;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import mekanism.api.recipes.MetallurgicInfuserRecipe;
import mekanism.api.recipes.PaintingRecipe;
import mekanism.api.recipes.chemical.ItemStackChemicalToItemStackRecipe;
import net.minecraft.world.item.crafting.Recipe;

public abstract class ItemStackChemicalToItemStackRecipeHandler<RECIPE extends ItemStackChemicalToItemStackRecipe<?, ?, ?>> extends MekanismRecipeHandler<RECIPE> {

    @Override
    public String dumpToCommandString(IRecipeManager manager, RECIPE recipe) {
        return buildCommandString(manager, recipe, recipe.getItemInput(), recipe.getChemicalInput(), recipe.getOutputDefinition());
    }

    @Override
    public <U extends Recipe<?>> boolean doesConflict(IRecipeManager manager, RECIPE recipe, U o) {
        //Only support if the other is an itemstack chemical to itemstack recipe and don't bother checking the reverse as the recipe type's generics
        // ensures that it is of the same type
        if (o instanceof ItemStackChemicalToItemStackRecipe<?, ?, ?> other) {
            //Check chemical ingredients first in case the type doesn't match
            return chemicalIngredientConflicts(recipe.getChemicalInput(), other.getChemicalInput()) &&
                   ingredientConflicts(recipe.getItemInput(), other.getItemInput());
        }
        return false;
    }

    @IRecipeHandler.For(ItemStackGasToItemStackRecipe.class)
    public static class ItemStackGasToItemStackRecipeHandler extends ItemStackChemicalToItemStackRecipeHandler<ItemStackGasToItemStackRecipe> {
    }

    @IRecipeHandler.For(MetallurgicInfuserRecipe.class)
    public static class MetallurgicInfuserRecipeHandler extends ItemStackChemicalToItemStackRecipeHandler<MetallurgicInfuserRecipe> {
    }

    @IRecipeHandler.For(PaintingRecipe.class)
    public static class PaintingRecipeHandler extends ItemStackChemicalToItemStackRecipeHandler<PaintingRecipe> {
    }
}