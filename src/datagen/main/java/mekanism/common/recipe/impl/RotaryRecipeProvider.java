package mekanism.common.recipe.impl;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import mekanism.api.datagen.recipe.builder.RotaryRecipeBuilder;
import mekanism.api.providers.IFluidProvider;
import mekanism.api.providers.IGasProvider;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.Mekanism;
import mekanism.common.recipe.ISubRecipeProvider;
import mekanism.common.registries.MekanismFluids;
import mekanism.common.registries.MekanismGases;
import mekanism.common.tags.MekanismTags;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

class RotaryRecipeProvider implements ISubRecipeProvider {

    @Override
    public void addRecipes(Consumer<FinishedRecipe> consumer) {
        String basePath = "rotary/";
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.BRINE, MekanismFluids.BRINE, MekanismTags.Fluids.BRINE);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.CHLORINE, MekanismFluids.CHLORINE, MekanismTags.Fluids.CHLORINE);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.ETHENE, MekanismFluids.ETHENE, MekanismTags.Fluids.ETHENE);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.HYDROGEN, MekanismFluids.HYDROGEN, MekanismTags.Fluids.HYDROGEN);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.HYDROGEN_CHLORIDE, MekanismFluids.HYDROGEN_CHLORIDE, MekanismTags.Fluids.HYDROGEN_CHLORIDE);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.LITHIUM, MekanismFluids.LITHIUM, MekanismTags.Fluids.LITHIUM);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.OXYGEN, MekanismFluids.OXYGEN, MekanismTags.Fluids.OXYGEN);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.SODIUM, MekanismFluids.SODIUM, MekanismTags.Fluids.SODIUM);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.SUPERHEATED_SODIUM, MekanismFluids.SUPERHEATED_SODIUM, MekanismTags.Fluids.SUPERHEATED_SODIUM);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.STEAM, MekanismFluids.STEAM, MekanismTags.Fluids.STEAM);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.SULFUR_DIOXIDE, MekanismFluids.SULFUR_DIOXIDE, MekanismTags.Fluids.SULFUR_DIOXIDE);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.SULFUR_TRIOXIDE, MekanismFluids.SULFUR_TRIOXIDE, MekanismTags.Fluids.SULFUR_TRIOXIDE);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.SULFURIC_ACID, MekanismFluids.SULFURIC_ACID, MekanismTags.Fluids.SULFURIC_ACID);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.HYDROFLUORIC_ACID, MekanismFluids.HYDROFLUORIC_ACID, MekanismTags.Fluids.HYDROFLUORIC_ACID);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.URANIUM_OXIDE, MekanismFluids.URANIUM_OXIDE, MekanismTags.Fluids.URANIUM_OXIDE);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.URANIUM_HEXAFLUORIDE, MekanismFluids.URANIUM_HEXAFLUORIDE, MekanismTags.Fluids.URANIUM_HEXAFLUORIDE);
        addRotaryCondensentratorRecipe(consumer, basePath, MekanismGases.WATER_VAPOR, new IFluidProvider() {
            @Nonnull
            @Override
            public Fluid getFluid() {
                return Fluids.WATER;
            }
        }, FluidTags.WATER);
    }

    private void addRotaryCondensentratorRecipe(Consumer<FinishedRecipe> consumer, String basePath, IGasProvider gas, IFluidProvider fluidOutput, TagKey<Fluid> fluidInput) {
        RotaryRecipeBuilder.rotary(
              IngredientCreatorAccess.fluid().from(fluidInput, 1),
              IngredientCreatorAccess.gas().from(gas, 1),
              gas.getStack(1),
              fluidOutput.getFluidStack(1)
        ).build(consumer, Mekanism.rl(basePath + gas.getName()));
    }
}