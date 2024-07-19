package mekanism.common.content.gear.mekatool;

import javax.annotation.Nonnull;
import mekanism.api.gear.EnchantmentBasedModule;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class ModuleSilkTouchUnit extends EnchantmentBasedModule<ModuleSilkTouchUnit> {

    @Nonnull
    @Override
    public Enchantment getEnchantment() {
        return Enchantments.SILK_TOUCH;
    }
}