package mekanism.common;

import javax.annotation.Nonnull;
import mekanism.common.registries.MekanismItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CreativeTabMekanism extends CreativeModeTab {

    public CreativeTabMekanism() {
        super(Mekanism.MODID);
    }

    @Nonnull
    @Override
    public ItemStack makeIcon() {
        return MekanismItems.ATOMIC_ALLOY.getItemStack();
    }

    @Nonnull
    @Override
    public Component getDisplayName() {
        //Overwrite the lang key to match the one representing Mekanism
        return MekanismLang.MEKANISM.translate();
    }
}