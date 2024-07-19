package mekanism.common.recipe.impl;

import javax.annotation.Nonnull;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismRecipeSerializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class SmeltingIRecipe extends ItemStackToItemStackRecipe {

    public SmeltingIRecipe(ResourceLocation id, ItemStackIngredient input, ItemStack output) {
        super(id, input, output);
    }

    @Nonnull
    @Override
    public RecipeType<ItemStackToItemStackRecipe> getType() {
        return MekanismRecipeType.SMELTING.get();
    }

    @Nonnull
    @Override
    public RecipeSerializer<ItemStackToItemStackRecipe> getSerializer() {
        return MekanismRecipeSerializers.SMELTING.get();
    }

    @Nonnull
    @Override
    public String getGroup() {
        return MekanismBlocks.ENERGIZED_SMELTER.getName();
    }

    @Nonnull
    @Override
    public ItemStack getToastSymbol() {
        return MekanismBlocks.ENERGIZED_SMELTER.getItemStack();
    }
}