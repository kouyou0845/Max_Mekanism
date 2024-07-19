package mekanism.common.recipe.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.common.DataGenJsonConstants;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ExtendedShapelessRecipeBuilder extends BaseRecipeBuilder<ExtendedShapelessRecipeBuilder> {

    private final List<Ingredient> ingredients = new ArrayList<>();

    private ExtendedShapelessRecipeBuilder(ItemLike result, int count) {
        super(RecipeSerializer.SHAPELESS_RECIPE, result, count);
    }

    public static ExtendedShapelessRecipeBuilder shapelessRecipe(ItemLike result) {
        return shapelessRecipe(result, 1);
    }

    public static ExtendedShapelessRecipeBuilder shapelessRecipe(ItemLike result, int count) {
        return new ExtendedShapelessRecipeBuilder(result, count);
    }

    public ExtendedShapelessRecipeBuilder addIngredient(TagKey<Item> tag) {
        return addIngredient(tag, 1);
    }

    public ExtendedShapelessRecipeBuilder addIngredient(TagKey<Item> tag, int quantity) {
        return addIngredient(Ingredient.of(tag), quantity);
    }

    public ExtendedShapelessRecipeBuilder addIngredient(ItemLike item) {
        return addIngredient(item, 1);
    }

    public ExtendedShapelessRecipeBuilder addIngredient(ItemLike item, int quantity) {
        return addIngredient(Ingredient.of(item), quantity);
    }

    public ExtendedShapelessRecipeBuilder addIngredient(Ingredient ingredient) {
        return addIngredient(ingredient, 1);
    }

    public ExtendedShapelessRecipeBuilder addIngredient(Ingredient ingredient, int quantity) {
        for (int i = 0; i < quantity; ++i) {
            ingredients.add(ingredient);
        }
        return this;
    }

    @Override
    protected void validate(ResourceLocation id) {
        if (ingredients.isEmpty()) {
            throw new IllegalStateException("Shapeless recipe '" + id + "' must have at least one ingredient!");
        }
    }

    @Override
    protected RecipeResult getResult(ResourceLocation id) {
        return new Result(id);
    }

    public class Result extends BaseRecipeResult {

        public Result(ResourceLocation id) {
            super(id);
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            super.serializeRecipeData(json);
            JsonArray jsonIngredients = new JsonArray();
            for (Ingredient ingredient : ingredients) {
                jsonIngredients.add(ingredient.toJson());
            }
            json.add(DataGenJsonConstants.INGREDIENTS, jsonIngredients);
        }
    }
}