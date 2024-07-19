package mekanism.common.recipe.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nonnull;
import mekanism.api.JsonConstants;
import mekanism.api.SerializerHelper;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.Mekanism;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistryEntry;

public class SawmillRecipeSerializer<RECIPE extends SawmillRecipe> extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<RECIPE> {

    private final IFactory<RECIPE> factory;

    public SawmillRecipeSerializer(IFactory<RECIPE> factory) {
        this.factory = factory;
    }

    @Nonnull
    @Override
    public RECIPE fromJson(@Nonnull ResourceLocation recipeId, @Nonnull JsonObject json) {
        JsonElement input = GsonHelper.isArrayNode(json, JsonConstants.INPUT) ? GsonHelper.getAsJsonArray(json, JsonConstants.INPUT) :
                            GsonHelper.getAsJsonObject(json, JsonConstants.INPUT);
        ItemStackIngredient inputIngredient = IngredientCreatorAccess.item().deserialize(input);
        ItemStack mainOutput = ItemStack.EMPTY;
        ItemStack secondaryOutput = ItemStack.EMPTY;
        double secondaryChance = 0;
        if (json.has(JsonConstants.SECONDARY_OUTPUT) || json.has(JsonConstants.SECONDARY_CHANCE)) {
            if (json.has(JsonConstants.MAIN_OUTPUT)) {
                //Allow for the main output to be optional if we have a secondary output
                mainOutput = SerializerHelper.getItemStack(json, JsonConstants.MAIN_OUTPUT);
                if (mainOutput.isEmpty()) {
                    throw new JsonSyntaxException("Sawmill main recipe output must not be empty, if it is defined.");
                }
            }
            //If we have either json element for secondary information, assume we have both and fail if we can't get one of them
            JsonElement chance = json.get(JsonConstants.SECONDARY_CHANCE);
            if (!GsonHelper.isNumberValue(chance)) {
                throw new JsonSyntaxException("Expected secondaryChance to be a number greater than zero.");
            }
            secondaryChance = chance.getAsJsonPrimitive().getAsDouble();
            if (secondaryChance <= 0 || secondaryChance > 1) {
                throw new JsonSyntaxException("Expected secondaryChance to be greater than zero, and less than or equal to one.");
            }
            secondaryOutput = SerializerHelper.getItemStack(json, JsonConstants.SECONDARY_OUTPUT);
            if (secondaryOutput.isEmpty()) {
                throw new JsonSyntaxException("Sawmill secondary recipe output must not be empty, if there is no main output.");
            }
        } else {
            //If we don't have a secondary output require a main output
            mainOutput = SerializerHelper.getItemStack(json, JsonConstants.MAIN_OUTPUT);
            if (mainOutput.isEmpty()) {
                throw new JsonSyntaxException("Sawmill main recipe output must not be empty, if there is no secondary output.");
            }
        }
        return this.factory.create(recipeId, inputIngredient, mainOutput, secondaryOutput, secondaryChance);
    }

    @Override
    public RECIPE fromNetwork(@Nonnull ResourceLocation recipeId, @Nonnull FriendlyByteBuf buffer) {
        try {
            ItemStackIngredient inputIngredient = IngredientCreatorAccess.item().read(buffer);
            ItemStack mainOutput = buffer.readItem();
            ItemStack secondaryOutput = buffer.readItem();
            double secondaryChance = buffer.readDouble();
            return this.factory.create(recipeId, inputIngredient, mainOutput, secondaryOutput, secondaryChance);
        } catch (Exception e) {
            Mekanism.logger.error("Error reading sawmill recipe from packet.", e);
            throw e;
        }
    }

    @Override
    public void toNetwork(@Nonnull FriendlyByteBuf buffer, @Nonnull RECIPE recipe) {
        try {
            recipe.write(buffer);
        } catch (Exception e) {
            Mekanism.logger.error("Error writing sawmill recipe to packet.", e);
            throw e;
        }
    }

    @FunctionalInterface
    public interface IFactory<RECIPE extends SawmillRecipe> {

        RECIPE create(ResourceLocation id, ItemStackIngredient input, ItemStack mainOutput, ItemStack secondaryOutput, double secondaryChance);
    }
}