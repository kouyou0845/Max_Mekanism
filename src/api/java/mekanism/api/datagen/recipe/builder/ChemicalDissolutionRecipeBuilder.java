package mekanism.api.datagen.recipe.builder;

import com.google.gson.JsonObject;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.api.JsonConstants;
import mekanism.api.SerializerHelper;
import mekanism.api.annotations.FieldsAreNonnullByDefault;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.merged.BoxedChemicalStack;
import mekanism.api.datagen.recipe.MekanismRecipeBuilder;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient.GasStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ChemicalDissolutionRecipeBuilder extends MekanismRecipeBuilder<ChemicalDissolutionRecipeBuilder> {

    private final ItemStackIngredient itemInput;
    private final GasStackIngredient gasInput;
    private final BoxedChemicalStack output;

    protected ChemicalDissolutionRecipeBuilder(ResourceLocation serializerName, ItemStackIngredient itemInput, GasStackIngredient gasInput,
          ChemicalStack<?> output) {
        super(serializerName);
        this.itemInput = itemInput;
        this.gasInput = gasInput;
        this.output = BoxedChemicalStack.box(output);
    }

    /**
     * Creates a Chemical Dissolution recipe builder.
     *
     * @param itemInput Item Input.
     * @param gasInput  Gas Input.
     * @param output    Output.
     */
    public static ChemicalDissolutionRecipeBuilder dissolution(ItemStackIngredient itemInput, GasStackIngredient gasInput, ChemicalStack<?> output) {
        if (output.isEmpty()) {
            throw new IllegalArgumentException("This dissolution chamber recipe requires a non empty chemical output.");
        }
        return new ChemicalDissolutionRecipeBuilder(mekSerializer("dissolution"), itemInput, gasInput, output);
    }

    @Override
    protected ChemicalDissolutionRecipeResult getResult(ResourceLocation id) {
        return new ChemicalDissolutionRecipeResult(id);
    }

    public class ChemicalDissolutionRecipeResult extends RecipeResult {

        protected ChemicalDissolutionRecipeResult(ResourceLocation id) {
            super(id);
        }

        @Override
        public void serializeRecipeData(@Nonnull JsonObject json) {
            json.add(JsonConstants.ITEM_INPUT, itemInput.serialize());
            json.add(JsonConstants.GAS_INPUT, gasInput.serialize());
            json.add(JsonConstants.OUTPUT, SerializerHelper.serializeBoxedChemicalStack(output));
        }
    }
}