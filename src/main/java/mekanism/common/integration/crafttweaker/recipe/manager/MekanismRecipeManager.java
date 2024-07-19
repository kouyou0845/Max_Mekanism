package mekanism.common.integration.crafttweaker.recipe.manager;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.action.recipe.ActionAddRecipe;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import java.util.List;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.common.integration.crafttweaker.CrTConstants;
import mekanism.common.integration.crafttweaker.CrTUtils;
import mekanism.common.integration.crafttweaker.chemical.ICrTChemicalStack;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fluids.FluidStack;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name(CrTConstants.CLASS_RECIPE_MANAGER)
public abstract class MekanismRecipeManager<RECIPE extends MekanismRecipe> implements IRecipeManager<RECIPE> {

    private final IMekanismRecipeTypeProvider<RECIPE, ?> recipeType;

    protected MekanismRecipeManager(IMekanismRecipeTypeProvider<RECIPE, ?> recipeType) {
        this.recipeType = recipeType;
    }

    protected abstract ActionAddMekanismRecipe getAction(RECIPE recipe);

    protected void addRecipe(RECIPE recipe) {
        CraftTweakerAPI.apply(getAction(recipe));
    }

    @Override
    public RecipeType<RECIPE> getRecipeType() {
        return recipeType.getRecipeType();
    }

    @Override
    public ResourceLocation getBracketResourceLocation() {
        //Short circuit reverse lookup and just grab it from our recipe type
        return recipeType.getRegistryName();
    }

    @Override
    @Deprecated
    public List<RECIPE> getRecipesByOutput(IIngredient output) {
        throw new UnsupportedOperationException("Mekanism's recipe managers don't support reverse lookup by output, please lookup by recipe name.");
    }

    @Override
    @Deprecated
    public void remove(IIngredient output) {
        throw new UnsupportedOperationException("Mekanism's recipe managers don't support removal by output, please remove by recipe name.");
    }

    protected ResourceLocation getAndValidateName(String path) {
        return CrTUtils.rl(fixRecipeName(path));
    }

    protected ItemStack getAndValidateNotEmpty(IItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Output stack cannot be empty.");
        }
        return stack.getImmutableInternal();
    }

    protected FluidStack getAndValidateNotEmpty(IFluidStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Output stack cannot be empty.");
        }
        return stack.getImmutableInternal();
    }

    protected <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> STACK getAndValidateNotEmpty(ICrTChemicalStack<CHEMICAL, STACK, ?> stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Output stack cannot be empty.");
        }
        return stack.getImmutableInternal();
    }

    protected abstract class ActionAddMekanismRecipe extends ActionAddRecipe<RECIPE> {

        protected ActionAddMekanismRecipe(RECIPE recipe) {
            super(MekanismRecipeManager.this, recipe);
        }

        @Override//Force implementers to describe the outputs
        protected abstract String describeOutputs();
    }
}