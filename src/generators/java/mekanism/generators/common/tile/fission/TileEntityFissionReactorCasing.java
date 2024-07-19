package mekanism.generators.common.tile.fission;

import javax.annotation.Nonnull;
import mekanism.api.NBTConstants;
import mekanism.api.providers.IBlockProvider;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import mekanism.common.util.NBTUtils;
import mekanism.generators.common.MekanismGenerators;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import mekanism.generators.common.registries.GeneratorsBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityFissionReactorCasing extends TileEntityMultiblock<FissionReactorMultiblockData> {

    private boolean handleSound;
    private boolean prevBurning;

    public TileEntityFissionReactorCasing(BlockPos pos, BlockState state) {
        super(GeneratorsBlocks.FISSION_REACTOR_CASING, pos, state);
    }

    public TileEntityFissionReactorCasing(IBlockProvider blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
    }

    @Override
    protected boolean onUpdateServer(FissionReactorMultiblockData multiblock) {
        boolean needsPacket = super.onUpdateServer(multiblock);
        boolean burning = multiblock.isFormed() && multiblock.handlesSound(this) && multiblock.isBurning();
        if (burning != prevBurning) {
            prevBurning = burning;
            needsPacket = true;
        }
        return needsPacket;
    }

    public double getBoilEfficiency() {
        return (double) Math.round(getMultiblock().getBoilEfficiency() * 1_000) / 1_000;
    }

    public void setReactorActive(boolean active) {
        getMultiblock().setActive(active);
    }

    public Component getDamageString() {
        return MekanismLang.GENERIC_PERCENT.translate(getMultiblock().getDamagePercent());
    }

    public EnumColor getDamageColor() {
        double damage = getMultiblock().reactorDamage / FissionReactorMultiblockData.MAX_DAMAGE;
        return damage < 0.25 ? EnumColor.BRIGHT_GREEN : (damage < 0.5 ? EnumColor.YELLOW : (damage < 0.75 ? EnumColor.ORANGE : EnumColor.DARK_RED));
    }

    public EnumColor getTempColor() {
        double temp = getMultiblock().heatCapacitor.getTemperature();
        return temp < 600 ? EnumColor.BRIGHT_GREEN : (temp < 1_000 ? EnumColor.YELLOW :
                                                      (temp < 1_200 ? EnumColor.ORANGE : (temp < 1_600 ? EnumColor.RED : EnumColor.DARK_RED)));
    }

    public void setRateLimitFromPacket(double rate) {
        getMultiblock().setRateLimit(rate);
    }

    @Override
    public FissionReactorMultiblockData createMultiblock() {
        return new FissionReactorMultiblockData(this);
    }

    @Override
    public MultiblockManager<FissionReactorMultiblockData> getManager() {
        return MekanismGenerators.fissionReactorManager;
    }

    @Override
    protected boolean canPlaySound() {
        FissionReactorMultiblockData multiblock = getMultiblock();
        return multiblock.isFormed() && multiblock.isBurning() && handleSound;
    }

    @Nonnull
    @Override
    public CompoundTag getReducedUpdateTag() {
        CompoundTag updateTag = super.getReducedUpdateTag();
        FissionReactorMultiblockData multiblock = getMultiblock();
        updateTag.putBoolean(NBTConstants.HANDLE_SOUND, multiblock.isFormed() && multiblock.handlesSound(this));
        if (multiblock.isFormed()) {
            updateTag.putDouble(NBTConstants.BURNING, multiblock.lastBurnRate);
        }
        return updateTag;
    }

    @Override
    public void handleUpdateTag(@Nonnull CompoundTag tag) {
        super.handleUpdateTag(tag);
        NBTUtils.setBooleanIfPresent(tag, NBTConstants.HANDLE_SOUND, value -> handleSound = value);
        FissionReactorMultiblockData multiblock = getMultiblock();
        if (multiblock.isFormed()) {
            NBTUtils.setDoubleIfPresent(tag, NBTConstants.BURNING, value -> multiblock.lastBurnRate = value);
        }
    }
}
