package mekanism.client.jei;

import java.util.ArrayList;
import java.util.List;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.inventory.container.tile.FormulaicAssemblicatorContainer;
import mekanism.common.inventory.slot.FormulaicCraftingSlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;

public class FormulaicRecipeTransferInfo implements IRecipeTransferInfo<FormulaicAssemblicatorContainer, CraftingRecipe> {

    @Override
    public Class<FormulaicAssemblicatorContainer> getContainerClass() {
        return FormulaicAssemblicatorContainer.class;
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    @SuppressWarnings("removal")
    @Deprecated(forRemoval = true)
    public ResourceLocation getRecipeCategoryUid() {
        return getRecipeType().getUid();
    }

    @Override
    public boolean canHandle(FormulaicAssemblicatorContainer container, CraftingRecipe recipe) {
        return true;
    }

    @Override
    @SuppressWarnings("removal")
    @Deprecated(forRemoval = true)
    public Class<CraftingRecipe> getRecipeClass() {
        return CraftingRecipe.class;
    }

    @Override
    public List<Slot> getRecipeSlots(FormulaicAssemblicatorContainer container, CraftingRecipe recipe) {
        List<Slot> slots = new ArrayList<>();
        for (InventoryContainerSlot slot : container.getInventoryContainerSlots()) {
            if (slot.getInventorySlot() instanceof FormulaicCraftingSlot) {
                slots.add(slot);
            }
        }
        return slots;
    }

    @Override
    public List<Slot> getInventorySlots(FormulaicAssemblicatorContainer container, CraftingRecipe recipe) {
        List<Slot> slots = new ArrayList<>();
        slots.addAll(container.getMainInventorySlots());
        slots.addAll(container.getHotBarSlots());
        for (InventoryContainerSlot slot : container.getInventoryContainerSlots()) {
            if (slot.getInventorySlot() instanceof InputInventorySlot) {
                slots.add(slot);
            }
        }
        return slots;
    }
}