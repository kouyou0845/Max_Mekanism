package mekanism.api.datagen.recipe.builder;

import com.google.gson.JsonObject;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.api.JsonConstants;
import mekanism.api.SerializerHelper;
import mekanism.api.annotations.FieldsAreNonnullByDefault;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.datagen.recipe.MekanismRecipeBuilder;
import mekanism.api.math.FloatingLong;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ElectrolysisRecipeBuilder extends MekanismRecipeBuilder<ElectrolysisRecipeBuilder> {

    private final FluidStackIngredient input;
    private final GasStack leftGasOutput;
    private final GasStack rightGasOutput;
    private FloatingLong energyMultiplier = FloatingLong.ONE;

    protected ElectrolysisRecipeBuilder(FluidStackIngredient input, GasStack leftGasOutput, GasStack rightGasOutput) {
        super(mekSerializer("separating"));
        this.input = input;
        this.leftGasOutput = leftGasOutput;
        this.rightGasOutput = rightGasOutput;
    }

    /**
     * Creates a Separating recipe builder.
     *
     * @param input          Input.
     * @param leftGasOutput  Left Output.
     * @param rightGasOutput Right Output.
     */
    public static ElectrolysisRecipeBuilder separating(FluidStackIngredient input, GasStack leftGasOutput, GasStack rightGasOutput) {
        if (leftGasOutput.isEmpty() || rightGasOutput.isEmpty()) {
            throw new IllegalArgumentException("This separating recipe requires non empty gas outputs.");
        }
        return new ElectrolysisRecipeBuilder(input, leftGasOutput, rightGasOutput);
    }

    /**
     * Sets the energy multiplier for this recipe.
     *
     * @param multiplier Multiplier to the energy cost in relation to the configured hydrogen separating energy cost. This value must be greater than or equal to one.
     */
    public ElectrolysisRecipeBuilder energyMultiplier(FloatingLong multiplier) {
        if (multiplier.smallerThan(FloatingLong.ONE)) {
            throw new IllegalArgumentException("Energy multiplier must be greater than or equal to one");
        }
        this.energyMultiplier = multiplier;
        return this;
    }

    @Override
    protected ElectrolysisRecipeResult getResult(ResourceLocation id) {
        return new ElectrolysisRecipeResult(id);
    }

    public class ElectrolysisRecipeResult extends RecipeResult {

        protected ElectrolysisRecipeResult(ResourceLocation id) {
            super(id);
        }

        @Override
        public void serializeRecipeData(@Nonnull JsonObject json) {
            json.add(JsonConstants.INPUT, input.serialize());
            if (energyMultiplier.greaterThan(FloatingLong.ONE)) {
                //Only add energy usage if it is greater than one, as otherwise it will default to one
                json.addProperty(JsonConstants.ENERGY_MULTIPLIER, energyMultiplier);
            }
            json.add(JsonConstants.LEFT_GAS_OUTPUT, SerializerHelper.serializeGasStack(leftGasOutput));
            json.add(JsonConstants.RIGHT_GAS_OUTPUT, SerializerHelper.serializeGasStack(rightGasOutput));
        }
    }
}