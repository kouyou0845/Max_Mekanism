package mekanism.common.recipe.impl;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.api.recipes.ItemStackToFluidRecipe;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.Mekanism;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fluids.FluidStack;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class NutritionalLiquifierIRecipe extends ItemStackToFluidRecipe {

    public NutritionalLiquifierIRecipe(Item item, ItemStackIngredient input, FluidStack output) {
        this(Mekanism.rl("liquifier/" + item.getRegistryName().toString().replace(':', '/')), input, output);
    }

    public NutritionalLiquifierIRecipe(ResourceLocation id, ItemStackIngredient input, FluidStack output) {
        super(id, input, output);
        //TODO - V11: Make the recipe system support a concept similar to vanilla's "special recipe". The backend already exists
        // but we don't currently have a way for it to get registered and added to the list. getType and getSerializer are nonnull,
        // but return a null value due to us not having a good way to handle this. It doesn't matter as they don't get synced across
        // the network and always exist, but we should improve this so that they do get properly implemented
    }

    @Nonnull
    @Override
    public RecipeType<ItemStackToFluidRecipe> getType() {
        return null;
    }

    @Nonnull
    @Override
    public RecipeSerializer<ItemStackToFluidRecipe> getSerializer() {
        return null;
    }

    @Nonnull
    @Override
    public String getGroup() {
        return MekanismBlocks.NUTRITIONAL_LIQUIFIER.getName();
    }

    @Nonnull
    @Override
    public ItemStack getToastSymbol() {
        return MekanismBlocks.NUTRITIONAL_LIQUIFIER.getItemStack();
    }
}