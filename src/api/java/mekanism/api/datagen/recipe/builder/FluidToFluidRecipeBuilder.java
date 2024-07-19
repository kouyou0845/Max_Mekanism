package mekanism.api.datagen.recipe.builder;

import com.google.gson.JsonObject;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.api.JsonConstants;
import mekanism.api.SerializerHelper;
import mekanism.api.annotations.FieldsAreNonnullByDefault;
import mekanism.api.datagen.recipe.MekanismRecipeBuilder;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidToFluidRecipeBuilder extends MekanismRecipeBuilder<FluidToFluidRecipeBuilder> {

    private final FluidStackIngredient input;
    private final FluidStack output;

    protected FluidToFluidRecipeBuilder(FluidStackIngredient input, FluidStack output) {
        super(mekSerializer("evaporating"));
        this.input = input;
        this.output = output;
    }

    /**
     * Creates an Evaporating recipe builder.
     *
     * @param input  Input.
     * @param output Output.
     */
    public static FluidToFluidRecipeBuilder evaporating(FluidStackIngredient input, FluidStack output) {
        if (output.isEmpty()) {
            throw new IllegalArgumentException("This evaporating recipe requires a non empty fluid output.");
        }
        return new FluidToFluidRecipeBuilder(input, output);
    }

    @Override
    protected FluidToFluidRecipeResult getResult(ResourceLocation id) {
        return new FluidToFluidRecipeResult(id);
    }

    public class FluidToFluidRecipeResult extends RecipeResult {

        protected FluidToFluidRecipeResult(ResourceLocation id) {
            super(id);
        }

        @Override
        public void serializeRecipeData(@Nonnull JsonObject json) {
            json.add(JsonConstants.INPUT, input.serialize());
            json.add(JsonConstants.OUTPUT, SerializerHelper.serializeFluidStack(output));
        }
    }
}