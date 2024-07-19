package mekanism.common.tile.transmitter;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.MekanismAPI;
import mekanism.api.NBTConstants;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.chemical.gas.attribute.GasAttributes.Radiation;
import mekanism.api.chemical.infuse.IInfusionTank;
import mekanism.api.chemical.pigment.IPigmentTank;
import mekanism.api.chemical.slurry.ISlurryTank;
import mekanism.api.math.MathUtils;
import mekanism.api.providers.IBlockProvider;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.TransmitterType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.DynamicHandler.InteractPredicate;
import mekanism.common.capabilities.chemical.dynamic.DynamicChemicalHandler.DynamicGasHandler;
import mekanism.common.capabilities.chemical.dynamic.DynamicChemicalHandler.DynamicInfusionHandler;
import mekanism.common.capabilities.chemical.dynamic.DynamicChemicalHandler.DynamicPigmentHandler;
import mekanism.common.capabilities.chemical.dynamic.DynamicChemicalHandler.DynamicSlurryHandler;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.resolver.manager.ChemicalHandlerManager.GasHandlerManager;
import mekanism.common.capabilities.resolver.manager.ChemicalHandlerManager.InfusionHandlerManager;
import mekanism.common.capabilities.resolver.manager.ChemicalHandlerManager.PigmentHandlerManager;
import mekanism.common.capabilities.resolver.manager.ChemicalHandlerManager.SlurryHandlerManager;
import mekanism.common.content.network.BoxedChemicalNetwork;
import mekanism.common.content.network.transmitter.BoxedPressurizedTube;
import mekanism.common.integration.computer.ComputerCapabilityHelper;
import mekanism.common.integration.computer.IComputerTile;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.lib.transmitter.ConnectionType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.interfaces.ITileRadioactive;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityPressurizedTube extends TileEntityTransmitter implements IComputerTile, ITileRadioactive {

    private final GasHandlerManager gasHandlerManager;
    private final InfusionHandlerManager infusionHandlerManager;
    private final PigmentHandlerManager pigmentHandlerManager;
    private final SlurryHandlerManager slurryHandlerManager;

    public TileEntityPressurizedTube(IBlockProvider blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        InteractPredicate canExtract = getExtractPredicate();
        InteractPredicate canInsert = getInsertPredicate();
        addCapabilityResolver(gasHandlerManager = new GasHandlerManager(getHolder(BoxedPressurizedTube::getGasTanks),
              new DynamicGasHandler(this::getGasTanks, canExtract, canInsert, null)));
        addCapabilityResolver(infusionHandlerManager = new InfusionHandlerManager(getHolder(BoxedPressurizedTube::getInfusionTanks),
              new DynamicInfusionHandler(this::getInfusionTanks, canExtract, canInsert, null)));
        addCapabilityResolver(pigmentHandlerManager = new PigmentHandlerManager(getHolder(BoxedPressurizedTube::getPigmentTanks),
              new DynamicPigmentHandler(this::getPigmentTanks, canExtract, canInsert, null)));
        addCapabilityResolver(slurryHandlerManager = new SlurryHandlerManager(getHolder(BoxedPressurizedTube::getSlurryTanks),
              new DynamicSlurryHandler(this::getSlurryTanks, canExtract, canInsert, null)));
        ComputerCapabilityHelper.addComputerCapabilities(this, this::addCapabilityResolver);
    }

    @Override
    protected BoxedPressurizedTube createTransmitter(IBlockProvider blockProvider) {
        return new BoxedPressurizedTube(blockProvider, this);
    }

    @Override
    public BoxedPressurizedTube getTransmitter() {
        return (BoxedPressurizedTube) super.getTransmitter();
    }

    @Override
    protected void onUpdateServer() {
        getTransmitter().pullFromAcceptors();
        super.onUpdateServer();
    }

    @Override
    public TransmitterType getTransmitterType() {
        return TransmitterType.PRESSURIZED_TUBE;
    }

    @Nonnull
    @Override
    protected BlockState upgradeResult(@Nonnull BlockState current, @Nonnull BaseTier tier) {
        return switch (tier) {
            case BASIC -> BlockStateHelper.copyStateData(current, MekanismBlocks.BASIC_PRESSURIZED_TUBE);
            case ADVANCED -> BlockStateHelper.copyStateData(current, MekanismBlocks.ADVANCED_PRESSURIZED_TUBE);
            case ELITE -> BlockStateHelper.copyStateData(current, MekanismBlocks.ELITE_PRESSURIZED_TUBE);
            case ULTIMATE -> BlockStateHelper.copyStateData(current, MekanismBlocks.ULTIMATE_PRESSURIZED_TUBE);
            default -> current;
        };
    }

    @Nonnull
    @Override
    public CompoundTag getUpdateTag() {
        //Note: We add the stored information to the initial update tag and not to the one we sync on side changes which uses getReducedUpdateTag
        CompoundTag updateTag = super.getUpdateTag();
        if (getTransmitter().hasTransmitterNetwork()) {
            BoxedChemicalNetwork network = getTransmitter().getTransmitterNetwork();
            updateTag.put(NBTConstants.BOXED_CHEMICAL, network.lastChemical.write(new CompoundTag()));
            updateTag.putFloat(NBTConstants.SCALE, network.currentScale);
        }
        return updateTag;
    }

    private <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>, TANK extends IChemicalTank<CHEMICAL, STACK>>
    IChemicalTankHolder<CHEMICAL, STACK, TANK> getHolder(BiFunction<BoxedPressurizedTube, Direction, List<TANK>> tankFunction) {
        BoxedPressurizedTube tube = getTransmitter();
        return direction -> {
            if (direction != null && tube.getConnectionTypeRaw(direction) == ConnectionType.NONE) {
                //If we actually have a side, and our connection type on that side is none, then return that we have no tanks
                return Collections.emptyList();
            }
            return tankFunction.apply(tube, direction);
        };
    }

    @Override
    public float getRadiationScale() {
        if (MekanismAPI.getRadiationManager().isRadiationEnabled()) {
            BoxedPressurizedTube tube = getTransmitter();
            if (isRemote()) {
                if (tube.hasTransmitterNetwork()) {
                    BoxedChemicalNetwork network = tube.getTransmitterNetwork();
                    if (!network.lastChemical.isEmpty() && !network.isTankEmpty() && network.lastChemical.getChemical().has(Radiation.class)) {
                        //Note: This may act as full when the network isn't actually full if there is radioactive stuff
                        // going through it, but it shouldn't matter too much
                        return network.currentScale;
                    }
                }
            } else {
                IGasTank gasTank = tube.getGasTank();
                if (!gasTank.isEmpty() && gasTank.getStack().has(Radiation.class)) {
                    return gasTank.getStored() / (float) gasTank.getCapacity();
                }
            }
        }
        return 0;
    }

    @Override
    public int getRadiationParticleCount() {
        return MathUtils.clampToInt(3 * getRadiationScale());
    }

    private List<IGasTank> getGasTanks(@Nullable Direction side) {
        return gasHandlerManager.getContainers(side);
    }

    private List<IInfusionTank> getInfusionTanks(@Nullable Direction side) {
        return infusionHandlerManager.getContainers(side);
    }

    private List<IPigmentTank> getPigmentTanks(@Nullable Direction side) {
        return pigmentHandlerManager.getContainers(side);
    }

    private List<ISlurryTank> getSlurryTanks(@Nullable Direction side) {
        return slurryHandlerManager.getContainers(side);
    }

    @Override
    public void sideChanged(@Nonnull Direction side, @Nonnull ConnectionType old, @Nonnull ConnectionType type) {
        super.sideChanged(side, old, type);
        if (type == ConnectionType.NONE) {
            invalidateCapability(Capabilities.GAS_HANDLER_CAPABILITY, side);
            invalidateCapability(Capabilities.INFUSION_HANDLER_CAPABILITY, side);
            invalidateCapability(Capabilities.PIGMENT_HANDLER_CAPABILITY, side);
            invalidateCapability(Capabilities.SLURRY_HANDLER_CAPABILITY, side);
            //Notify the neighbor on that side our state changed and we no longer have a capability
            WorldUtils.notifyNeighborOfChange(level, side, worldPosition);
        } else if (old == ConnectionType.NONE) {
            //Notify the neighbor on that side our state changed, and we now do have a capability
            WorldUtils.notifyNeighborOfChange(level, side, worldPosition);
        }
    }

    //Methods relating to IComputerTile
    @Override
    public String getComputerName() {
        return getTransmitter().getTier().getBaseTier().getLowerName() + "PressurizedTube";
    }

    @ComputerMethod
    private ChemicalStack<?> getBuffer() {
        return getTransmitter().getBufferWithFallback().getChemicalStack();
    }

    @ComputerMethod
    private long getCapacity() {
        BoxedPressurizedTube tube = getTransmitter();
        return tube.hasTransmitterNetwork() ? tube.getTransmitterNetwork().getCapacity() : tube.getCapacity();
    }

    @ComputerMethod
    private long getNeeded() {
        return getCapacity() - getBuffer().getAmount();
    }

    @ComputerMethod
    private double getFilledPercentage() {
        return getBuffer().getAmount() / (double) getCapacity();
    }
    //End methods IComputerTile
}