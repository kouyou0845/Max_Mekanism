package mekanism.common.recipe.impl;

import java.util.Map;
import java.util.function.Consumer;
import mekanism.api.datagen.recipe.builder.CombinerRecipeBuilder;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.Mekanism;
import mekanism.common.recipe.ISubRecipeProvider;
import mekanism.common.tags.MekanismTags;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;

class CombinerRecipeProvider implements ISubRecipeProvider {

    @Override
    public void addRecipes(Consumer<FinishedRecipe> consumer) {
        String basePath = "combining/";
        addCombinerDyeRecipes(consumer, basePath + "dye/");
        addCombinerGlowRecipes(consumer, basePath + "glow/");
        addCombinerWaxingRecipes(consumer, basePath + "wax/");
        //Gravel
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(Items.FLINT),
              IngredientCreatorAccess.item().from(Tags.Items.COBBLESTONE_NORMAL),
              new ItemStack(Blocks.GRAVEL)
        ).build(consumer, Mekanism.rl(basePath + "gravel"));
        //Obsidian
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(MekanismTags.Items.DUSTS_OBSIDIAN, 4),
              IngredientCreatorAccess.item().from(Tags.Items.COBBLESTONE_DEEPSLATE),
              new ItemStack(Blocks.OBSIDIAN)
        ).build(consumer, Mekanism.rl(basePath + "obsidian"));
        //Rooted Dirt
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(Items.HANGING_ROOTS, 3),
              IngredientCreatorAccess.item().from(Blocks.DIRT),
              new ItemStack(Blocks.ROOTED_DIRT)
        ).build(consumer, Mekanism.rl(basePath + "rooted_dirt"));
    }

    private void addCombinerDyeRecipes(Consumer<FinishedRecipe> consumer, String basePath) {
        //Black + white -> light gray
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(Tags.Items.DYES_BLACK),
              IngredientCreatorAccess.item().from(Tags.Items.DYES_WHITE, 2),
              new ItemStack(Items.LIGHT_GRAY_DYE, 6)
        ).build(consumer, Mekanism.rl(basePath + "black_to_light_gray"));
        //Blue + green -> cyan
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(Tags.Items.DYES_BLUE),
              IngredientCreatorAccess.item().from(Tags.Items.DYES_GREEN),
              new ItemStack(Items.CYAN_DYE, 4)
        ).build(consumer, Mekanism.rl(basePath + "cyan"));
        //Gray + white -> light gray
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(Tags.Items.DYES_GRAY),
              IngredientCreatorAccess.item().from(Tags.Items.DYES_WHITE),
              new ItemStack(Items.LIGHT_GRAY_DYE, 4)
        ).build(consumer, Mekanism.rl(basePath + "gray_to_light_gray"));
        //Blue + white -> light blue
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(Tags.Items.DYES_BLUE),
              IngredientCreatorAccess.item().from(Tags.Items.DYES_WHITE),
              new ItemStack(Items.LIGHT_BLUE_DYE, 4)
        ).build(consumer, Mekanism.rl(basePath + "light_blue"));
        //Green + white -> lime
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(Tags.Items.DYES_GREEN),
              IngredientCreatorAccess.item().from(Tags.Items.DYES_WHITE),
              new ItemStack(Items.LIME_DYE, 4)
        ).build(consumer, Mekanism.rl(basePath + "lime"));
        //Purple + pink -> magenta
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(Tags.Items.DYES_PURPLE),
              IngredientCreatorAccess.item().from(Tags.Items.DYES_PINK),
              new ItemStack(Items.MAGENTA_DYE, 4)
        ).build(consumer, Mekanism.rl(basePath + "magenta"));
        //Red + yellow -> orange
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(Tags.Items.DYES_RED),
              IngredientCreatorAccess.item().from(Tags.Items.DYES_YELLOW),
              new ItemStack(Items.ORANGE_DYE, 4)
        ).build(consumer, Mekanism.rl(basePath + "orange"));
        //Red + white -> pink
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(Tags.Items.DYES_RED),
              IngredientCreatorAccess.item().from(Tags.Items.DYES_WHITE),
              new ItemStack(Items.PINK_DYE, 4)
        ).build(consumer, Mekanism.rl(basePath + "pink"));
        //Blue + red -> purple
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(Tags.Items.DYES_BLUE),
              IngredientCreatorAccess.item().from(Tags.Items.DYES_RED),
              new ItemStack(Items.PURPLE_DYE, 4)
        ).build(consumer, Mekanism.rl(basePath + "purple"));
    }

    private void addCombinerGlowRecipes(Consumer<FinishedRecipe> consumer, String basePath) {
        ItemStackIngredient glow = IngredientCreatorAccess.item().from(Tags.Items.DUSTS_GLOWSTONE);
        //Sweet Berries -> Glow Berries
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(Items.SWEET_BERRIES),
              glow,
              new ItemStack(Items.GLOW_BERRIES)
        ).build(consumer, Mekanism.rl(basePath + "berries"));
        //Ink Sac -> Glow Ink Sac
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(Items.INK_SAC),
              glow,
              new ItemStack(Items.GLOW_INK_SAC)
        ).build(consumer, Mekanism.rl(basePath + "ink_sac"));
        //Item Frame -> Glow Item Frame
        CombinerRecipeBuilder.combining(
              IngredientCreatorAccess.item().from(Items.ITEM_FRAME),
              glow,
              new ItemStack(Items.GLOW_ITEM_FRAME)
        ).build(consumer, Mekanism.rl(basePath + "item_frame"));
    }

    private void addCombinerWaxingRecipes(Consumer<FinishedRecipe> consumer, String basePath) {
        //Generate baseline recipes from waxing recipe set
        ItemStackIngredient wax = IngredientCreatorAccess.item().from(Items.HONEYCOMB);
        for (Map.Entry<Block, Block> entry : HoneycombItem.WAXABLES.get().entrySet()) {
            Block result = entry.getValue();
            CombinerRecipeBuilder.combining(
                  IngredientCreatorAccess.item().from(entry.getKey()),
                  wax,
                  new ItemStack(result)
            ).build(consumer, Mekanism.rl(basePath + result.asItem()));
        }
    }
}