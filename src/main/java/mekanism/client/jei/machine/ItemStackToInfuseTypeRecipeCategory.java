package mekanism.client.jei.machine;

import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.recipes.ItemStackToInfuseTypeRecipe;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiInfusionGauge;
import mekanism.client.jei.MekanismJEI;
import mekanism.client.jei.MekanismJEIRecipeType;
import mekanism.common.MekanismLang;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.resources.ResourceLocation;

public class ItemStackToInfuseTypeRecipeCategory extends ItemStackToChemicalRecipeCategory<InfuseType, InfusionStack, ItemStackToInfuseTypeRecipe> {

    private static final ResourceLocation iconRL = MekanismUtils.getResource(ResourceType.GUI, "infuse_types.png");

    public ItemStackToInfuseTypeRecipeCategory(IGuiHelper helper, MekanismJEIRecipeType<ItemStackToInfuseTypeRecipe> recipeType) {
        super(helper, recipeType, MekanismLang.CONVERSION_INFUSION.translate(), createIcon(helper, iconRL), MekanismJEI.TYPE_INFUSION, true);
    }

    @Override
    protected GuiInfusionGauge getGauge(GaugeType type, int x, int y) {
        return GuiInfusionGauge.getDummy(type, this, x, y);
    }
}