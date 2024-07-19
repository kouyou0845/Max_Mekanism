package mekanism.common.item.block.machine;

import java.util.List;
import javax.annotation.Nonnull;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.block.prefab.BlockFactoryMachine.BlockFactory;
import mekanism.common.tier.FactoryTier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ItemBlockFactory extends ItemBlockMachine {

    public ItemBlockFactory(BlockFactory<?> block) {
        super(block);
    }

    @Override
    public FactoryTier getTier() {
        return Attribute.getTier(getBlock(), FactoryTier.class);
    }

    @Override
    protected void addTypeDetails(@Nonnull ItemStack stack, Level world, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        tooltip.add(MekanismLang.FACTORY_TYPE.translateColored(EnumColor.INDIGO, EnumColor.GRAY, Attribute.get(getBlock(), AttributeFactoryType.class).getFactoryType()));
        super.addTypeDetails(stack, world, tooltip, flag);
    }
}