package mekanism.common.item.gear;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.AutomationType;
import mekanism.api.IIncrementalEnum;
import mekanism.api.NBTConstants;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.math.MathUtils;
import mekanism.api.text.EnumColor;
import mekanism.api.text.IHasTextComponent;
import mekanism.api.text.ILangEntry;
import mekanism.client.render.RenderPropertiesProvider;
import mekanism.common.MekanismLang;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.ItemCapabilityWrapper.ItemCapability;
import mekanism.common.capabilities.chemical.item.RateLimitGasHandler;
import mekanism.common.config.MekanismConfig;
import mekanism.common.item.CapabilityItem;
import mekanism.common.item.interfaces.IGasItem;
import mekanism.common.item.interfaces.IItemHUDProvider;
import mekanism.common.item.interfaces.IModeItem;
import mekanism.common.registries.MekanismGases;
import mekanism.common.util.ChemicalUtil;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.StorageUtils;
import net.minecraft.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.IItemRenderProperties;

public class ItemFlamethrower extends CapabilityItem implements IItemHUDProvider, IModeItem, IGasItem {

    public ItemFlamethrower(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE).setNoRepair());
    }

    @Override
    public void initializeClient(@Nonnull Consumer<IItemRenderProperties> consumer) {
        consumer.accept(RenderPropertiesProvider.flamethrower());
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level world, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        StorageUtils.addStoredGas(stack, tooltip, true, false);
        tooltip.add(MekanismLang.MODE.translateColored(EnumColor.GRAY, getMode(stack)));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSame(oldStack, newStack);
    }

    @Override
    public boolean isBarVisible(@Nonnull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@Nonnull ItemStack stack) {
        return StorageUtils.getBarWidth(stack);
    }

    @Override
    public int getBarColor(@Nonnull ItemStack stack) {
        return ChemicalUtil.getRGBDurabilityForDisplay(stack);
    }

    @Override
    public void fillItemCategory(@Nonnull CreativeModeTab group, @Nonnull NonNullList<ItemStack> items) {
        super.fillItemCategory(group, items);
        if (allowdedIn(group)) {
            items.add(ChemicalUtil.getFilledVariant(new ItemStack(this), MekanismConfig.gear.flamethrowerMaxGas.get(), MekanismGases.HYDROGEN));
        }
    }

    public FlamethrowerMode getMode(ItemStack stack) {
        return FlamethrowerMode.byIndexStatic(ItemDataUtils.getInt(stack, NBTConstants.MODE));
    }

    public void setMode(ItemStack stack, FlamethrowerMode mode) {
        ItemDataUtils.setInt(stack, NBTConstants.MODE, mode.ordinal());
    }

    @Override
    protected void gatherCapabilities(List<ItemCapability> capabilities, ItemStack stack, CompoundTag nbt) {
        super.gatherCapabilities(capabilities, stack, nbt);
        capabilities.add(RateLimitGasHandler.create(MekanismConfig.gear.flamethrowerFillRate, MekanismConfig.gear.flamethrowerMaxGas,
              (item, automationType) -> automationType != AutomationType.EXTERNAL, ChemicalTankBuilder.GAS.alwaysTrueBi,
              gas -> gas == MekanismGases.HYDROGEN.getChemical()));
    }

    @Override
    public void addHUDStrings(List<Component> list, Player player, ItemStack stack, EquipmentSlot slotType) {
        boolean hasGas = false;
        Optional<IGasHandler> capability = stack.getCapability(Capabilities.GAS_HANDLER_CAPABILITY).resolve();
        if (capability.isPresent()) {
            IGasHandler gasHandlerItem = capability.get();
            if (gasHandlerItem.getTanks() > 0) {
                //Validate something didn't go terribly wrong, and we actually do have the tank we expect to have
                GasStack storedGas = gasHandlerItem.getChemicalInTank(0);
                if (!storedGas.isEmpty()) {
                    list.add(MekanismLang.FLAMETHROWER_STORED.translateColored(EnumColor.GRAY, EnumColor.ORANGE, storedGas.getAmount()));
                    hasGas = true;
                }
            }
        }
        if (!hasGas) {
            list.add(MekanismLang.FLAMETHROWER_STORED.translateColored(EnumColor.GRAY, EnumColor.ORANGE, MekanismLang.NO_GAS));
        }
        list.add(MekanismLang.MODE.translate(getMode(stack)));
    }

    @Override
    public void changeMode(@Nonnull Player player, @Nonnull ItemStack stack, int shift, boolean displayChangeMessage) {
        FlamethrowerMode mode = getMode(stack);
        FlamethrowerMode newMode = mode.adjust(shift);
        if (mode != newMode) {
            setMode(stack, newMode);
            if (displayChangeMessage) {
                player.sendMessage(MekanismUtils.logFormat(MekanismLang.FLAMETHROWER_MODE_CHANGE.translate(newMode)), Util.NIL_UUID);
            }
        }
    }

    @Nonnull
    @Override
    public Component getScrollTextComponent(@Nonnull ItemStack stack) {
        return getMode(stack).getTextComponent();
    }

    @Override
    public boolean isEnchantable(@Nonnull ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    public enum FlamethrowerMode implements IIncrementalEnum<FlamethrowerMode>, IHasTextComponent {
        COMBAT(MekanismLang.FLAMETHROWER_COMBAT, EnumColor.YELLOW),
        HEAT(MekanismLang.FLAMETHROWER_HEAT, EnumColor.ORANGE),
        INFERNO(MekanismLang.FLAMETHROWER_INFERNO, EnumColor.DARK_RED);

        private static final FlamethrowerMode[] MODES = values();
        private final ILangEntry langEntry;
        private final EnumColor color;

        FlamethrowerMode(ILangEntry langEntry, EnumColor color) {
            this.langEntry = langEntry;
            this.color = color;
        }

        @Override
        public Component getTextComponent() {
            return langEntry.translateColored(color);
        }

        @Nonnull
        @Override
        public FlamethrowerMode byIndex(int index) {
            return byIndexStatic(index);
        }

        public static FlamethrowerMode byIndexStatic(int index) {
            return MathUtils.getByIndexMod(MODES, index);
        }
    }
}