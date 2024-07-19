package mekanism.common.tile.factory;

import java.util.Arrays;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.IContentsListener;
import mekanism.api.NBTConstants;
import mekanism.api.Upgrade;
import mekanism.api.annotations.NonNull;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.math.MathUtils;
import mekanism.api.providers.IBlockProvider;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.cache.ItemStackConstantChemicalToItemStackCachedRecipe;
import mekanism.api.recipes.cache.ItemStackConstantChemicalToItemStackCachedRecipe.ChemicalUsageMultiplier;
import mekanism.api.recipes.inputs.ILongInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerChemicalTankWrapper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.slot.chemical.GasInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.lookup.IDoubleRecipeLookupHandler.ItemChemicalRecipeLookupHandler;
import mekanism.common.recipe.lookup.IRecipeLookupHandler.ConstantUsageRecipeLookupHandler;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.ItemChemical;
import mekanism.common.tile.interfaces.IHasDumpButton;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import mekanism.common.upgrade.AdvancedMachineUpgradeData;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.StatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

//Compressing, injecting, purifying
public class TileEntityItemStackGasToItemStackFactory extends TileEntityItemToItemFactory<ItemStackGasToItemStackRecipe> implements IHasDumpButton,
      ItemChemicalRecipeLookupHandler<Gas, GasStack, ItemStackGasToItemStackRecipe>, ConstantUsageRecipeLookupHandler {

    private static final Map<RecipeError, Boolean> TRACKED_ERROR_TYPES = Map.of(
          RecipeError.NOT_ENOUGH_ENERGY, true,
          RecipeError.NOT_ENOUGH_INPUT, false,
          RecipeError.NOT_ENOUGH_SECONDARY_INPUT, true,
          RecipeError.NOT_ENOUGH_OUTPUT_SPACE, false,
          RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT, false
    );

    private final ILongInputHandler<@NonNull GasStack> gasInputHandler;
    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getChemicalItem")
    private GasInventorySlot extraSlot;
    @WrappingComputerMethod(wrapper = ComputerChemicalTankWrapper.class, methodNames = {"getChemical", "getChemicalCapacity", "getChemicalNeeded",
                                                                                        "getChemicalFilledPercentage"})
    private IGasTank gasTank;
    private final ChemicalUsageMultiplier gasUsageMultiplier;
    private final long[] usedSoFar;
    private double gasPerTickMeanMultiplier = 1;
    private long baseTotalUsage;

    public TileEntityItemStackGasToItemStackFactory(IBlockProvider blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state, TRACKED_ERROR_TYPES);
        gasInputHandler = InputHelper.getConstantInputHandler(gasTank);
        configComponent.addSupported(TransmissionType.GAS);
        configComponent.setupInputConfig(TransmissionType.GAS, gasTank);
        baseTotalUsage = BASE_TICKS_REQUIRED;
        usedSoFar = new long[tier.processes];
        if (useStatisticalMechanics()) {
            //Note: Statistical mechanics works best by just using the mean gas usage we want to target
            // rather than adjusting the mean each time to try and reach a given target
            gasUsageMultiplier = (usedSoFar, operatingTicks) -> StatUtils.inversePoisson(gasPerTickMeanMultiplier);
        } else {
            gasUsageMultiplier = (usedSoFar, operatingTicks) -> {
                long baseRemaining = baseTotalUsage - usedSoFar;
                int remainingTicks = getTicksRequired() - operatingTicks;
                if (baseRemaining < remainingTicks) {
                    //If we already used more than we would need to use (due to removing speed upgrades or adding gas upgrades)
                    // then just don't use any gas this tick
                    return 0;
                } else if (baseRemaining == remainingTicks) {
                    return 1;
                }
                return Math.max(MathUtils.clampToLong(baseRemaining / (double) remainingTicks), 0);
            };
        }
    }

    @Nonnull
    @Override
    public IChemicalTankHolder<Gas, GasStack, IGasTank> getInitialGasTanks(IContentsListener listener) {
        ChemicalTankHelper<Gas, GasStack, IGasTank> builder = ChemicalTankHelper.forSideGasWithConfig(this::getDirection, this::getConfig);
        //If the tank's contents change make sure to call our extended content listener that also marks sorting as being needed
        // as maybe the valid recipes have changed, and we need to sort again and have all recipes know they may need to be rechecked
        // if they are not still valid
        builder.addTank(gasTank = ChemicalTankBuilder.GAS.input(TileEntityAdvancedElectricMachine.MAX_GAS * tier.processes, this::containsRecipeB,
              markAllMonitorsChanged(listener)));
        return builder.build();
    }

    @Override
    protected void addSlots(InventorySlotHelper builder, IContentsListener listener, IContentsListener updateSortingListener) {
        super.addSlots(builder, listener, updateSortingListener);
        //Note: We care about the gas tank not the slot when it comes to recipes and updating sorting
        builder.addSlot(extraSlot = GasInventorySlot.fillOrConvert(gasTank, this::getLevel, listener, 7, 57));
    }

    public IGasTank getGasTank() {
        return gasTank;
    }

    @Nullable
    @Override
    protected GasInventorySlot getExtraSlot() {
        return extraSlot;
    }

    @Override
    public boolean isValidInputItem(@Nonnull ItemStack stack) {
        return containsRecipeA(stack);
    }

    @Override
    protected int getNeededInput(ItemStackGasToItemStackRecipe recipe, ItemStack inputStack) {
        return MathUtils.clampToInt(recipe.getItemInput().getNeededAmount(inputStack));
    }

    @Override
    protected boolean isCachedRecipeValid(@Nullable CachedRecipe<ItemStackGasToItemStackRecipe> cached, @Nonnull ItemStack stack) {
        if (cached != null) {
            ItemStackGasToItemStackRecipe cachedRecipe = cached.getRecipe();
            return cachedRecipe.getItemInput().testType(stack) && (gasTank.isEmpty() || cachedRecipe.getChemicalInput().testType(gasTank.getType()));
        }
        return false;
    }

    @Override
    protected ItemStackGasToItemStackRecipe findRecipe(int process, @Nonnull ItemStack fallbackInput, @Nonnull IInventorySlot outputSlot,
          @Nullable IInventorySlot secondaryOutputSlot) {
        GasStack stored = gasTank.getStack();
        ItemStack output = outputSlot.getStack();
        //TODO: Give it something that is not empty when we don't have a stored gas stack for getting the output?
        return getRecipeType().getInputCache().findTypeBasedRecipe(level, fallbackInput, stored,
              recipe -> InventoryUtils.areItemsStackable(recipe.getOutput(fallbackInput, stored), output));
    }

    @Override
    protected void handleSecondaryFuel() {
        extraSlot.fillTankOrConvert();
    }

    @Nonnull
    @Override
    public IMekanismRecipeTypeProvider<ItemStackGasToItemStackRecipe, ItemChemical<Gas, GasStack, ItemStackGasToItemStackRecipe>> getRecipeType() {
        return switch (type) {
            case INJECTING -> MekanismRecipeType.INJECTING;
            case PURIFYING -> MekanismRecipeType.PURIFYING;
            //TODO: Make it so that it throws an error if it is not one of the three types
            default -> MekanismRecipeType.COMPRESSING;
        };
    }

    private boolean useStatisticalMechanics() {
        return type == FactoryType.INJECTING || type == FactoryType.PURIFYING;
    }

    @Nullable
    @Override
    public ItemStackGasToItemStackRecipe getRecipe(int cacheIndex) {
        return findFirstRecipe(inputHandlers[cacheIndex], gasInputHandler);
    }

    @Nonnull
    @Override
    public CachedRecipe<ItemStackGasToItemStackRecipe> createNewCachedRecipe(@Nonnull ItemStackGasToItemStackRecipe recipe, int cacheIndex) {
        return new ItemStackConstantChemicalToItemStackCachedRecipe<>(recipe, recheckAllRecipeErrors[cacheIndex], inputHandlers[cacheIndex], gasInputHandler,
              gasUsageMultiplier, used -> usedSoFar[cacheIndex] = used, outputHandlers[cacheIndex])
              .setErrorsChanged(errors -> errorTracker.onErrorsChanged(errors, cacheIndex))
              .setCanHolderFunction(() -> MekanismUtils.canFunction(this))
              .setActive(active -> setActiveState(active, cacheIndex))
              .setEnergyRequirements(energyContainer::getEnergyPerTick, energyContainer)
              .setRequiredTicks(this::getTicksRequired)
              .setOnFinish(this::markForSave)
              .setOperatingTicksChanged(operatingTicks -> progress[cacheIndex] = operatingTicks);
    }

    @Override
    public boolean hasSecondaryResourceBar() {
        return true;
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains(NBTConstants.USED_SO_FAR, Tag.TAG_LONG_ARRAY)) {
            long[] savedUsed = nbt.getLongArray(NBTConstants.USED_SO_FAR);
            if (tier.processes != savedUsed.length) {
                Arrays.fill(usedSoFar, 0);
            }
            for (int i = 0; i < tier.processes && i < savedUsed.length; i++) {
                usedSoFar[i] = savedUsed[i];
            }
        } else {
            Arrays.fill(usedSoFar, 0);
        }
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbtTags) {
        super.saveAdditional(nbtTags);
        nbtTags.put(NBTConstants.USED_SO_FAR, new LongArrayTag(Arrays.copyOf(usedSoFar, usedSoFar.length)));
    }

    @Override
    public long getSavedUsedSoFar(int cacheIndex) {
        return usedSoFar[cacheIndex];
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.SPEED || upgrade == Upgrade.GAS && supportsUpgrade(Upgrade.GAS)) {
            if (useStatisticalMechanics()) {
                gasPerTickMeanMultiplier = MekanismUtils.getGasPerTickMeanMultiplier(this);
            } else {
                baseTotalUsage = MekanismUtils.getBaseUsage(this, BASE_TICKS_REQUIRED);
            }
        }
    }

    @Override
    public void parseUpgradeData(@Nonnull IUpgradeData upgradeData) {
        if (upgradeData instanceof AdvancedMachineUpgradeData data) {
            //Generic factory upgrade data handling
            super.parseUpgradeData(upgradeData);
            //Copy the contents using NBT so that if it is not actually valid due to a reload we don't crash
            gasTank.deserializeNBT(data.stored.serializeNBT());
            extraSlot.deserializeNBT(data.gasSlot.serializeNBT());
            System.arraycopy(data.usedSoFar, 0, usedSoFar, 0, data.usedSoFar.length);
        } else {
            Mekanism.logger.warn("Unhandled upgrade data.", new Throwable());
        }
    }

    @Nonnull
    @Override
    public AdvancedMachineUpgradeData getUpgradeData() {
        return new AdvancedMachineUpgradeData(redstone, getControlType(), getEnergyContainer(), progress, usedSoFar, gasTank, extraSlot, energySlot, inputSlots, outputSlots,
              isSorting(), getComponents());
    }

    @Override
    public void dump() {
        gasTank.setEmpty();
    }

    //Methods relating to IComputerTile
    @ComputerMethod
    private void dumpChemical() throws ComputerException {
        validateSecurityIsPublic();
        dump();
    }
    //End methods IComputerTile
}