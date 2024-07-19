package mekanism.common.lib.multiblock;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.Action;
import mekanism.api.NBTConstants;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.chemical.infuse.IInfusionTank;
import mekanism.api.chemical.pigment.IPigmentTank;
import mekanism.api.chemical.slurry.ISlurryTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.fluid.IMekanismFluidHandler;
import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.inventory.IMekanismInventory;
import mekanism.common.capabilities.chemical.dynamic.IGasTracker;
import mekanism.common.capabilities.chemical.dynamic.IInfusionTracker;
import mekanism.common.capabilities.chemical.dynamic.IPigmentTracker;
import mekanism.common.capabilities.chemical.dynamic.ISlurryTracker;
import mekanism.common.capabilities.heat.ITileHeatHandler;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.inventory.container.sync.dynamic.ContainerSync;
import mekanism.common.lib.math.voxel.IShape;
import mekanism.common.lib.math.voxel.VoxelCuboid;
import mekanism.common.lib.math.voxel.VoxelCuboid.CuboidRelative;
import mekanism.common.lib.multiblock.FormationProtocol.StructureRequirement;
import mekanism.common.lib.multiblock.IValveHandler.ValveData;
import mekanism.common.lib.multiblock.MultiblockCache.CacheSubstance;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.NBTUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MultiblockData implements IMekanismInventory, IMekanismFluidHandler, IMekanismStrictEnergyHandler, ITileHeatHandler, IGasTracker, IInfusionTracker,
      IPigmentTracker, ISlurryTracker {

    public Set<BlockPos> locations = new ObjectOpenHashSet<>();
    /**
     * @apiNote This set is only used for purposes of caching all known valid inner blocks of a multiblock structure, for use in checking if we need to revalidate the
     * multiblock when something changes, cases we want to skip are inner nodes just changing state (for example, super heating elements being activated) This set is
     * not synced or checked anywhere (for things like equals) as it is only used on the server and isn't part of the structure's information. It also is not the most
     * accurate of checks that get done against this as there is no way to tell if the state actually changed or if the block changed entirely, but assuming no one is
     * replacing the blocks inside a multiblock (which is unsupported) it will handle it fine, and we can easily special-case it becoming air as having been "broken"
     */
    public Set<BlockPos> internalLocations = new ObjectOpenHashSet<>();
    public Set<ValveData> valves = new ObjectOpenHashSet<>();

    @ContainerSync(getter = "getVolume", setter = "setVolume")
    private int volume;

    public UUID inventoryID;

    public boolean hasMaster;

    @Nullable//may be null if structure has not been fully sent
    public BlockPos renderLocation;

    @ContainerSync
    private VoxelCuboid bounds = new VoxelCuboid(0, 0, 0);

    @ContainerSync
    private boolean formed;
    public boolean recheckStructure;

    private int currentRedstoneLevel;

    private final BooleanSupplier remoteSupplier;
    private final Supplier<Level> worldSupplier;

    protected final List<IInventorySlot> inventorySlots = new ArrayList<>();
    protected final List<IExtendedFluidTank> fluidTanks = new ArrayList<>();
    protected final List<IGasTank> gasTanks = new ArrayList<>();
    protected final List<IInfusionTank> infusionTanks = new ArrayList<>();
    protected final List<IPigmentTank> pigmentTanks = new ArrayList<>();
    protected final List<ISlurryTank> slurryTanks = new ArrayList<>();
    protected final List<IEnergyContainer> energyContainers = new ArrayList<>();
    protected final List<IHeatCapacitor> heatCapacitors = new ArrayList<>();

    private boolean dirty;

    public MultiblockData(BlockEntity tile) {
        remoteSupplier = () -> tile.getLevel().isClientSide();
        worldSupplier = tile::getLevel;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void resetDirty() {
        dirty = false;
    }

    public void markDirty() {
        dirty = true;
    }

    /**
     * Tick the multiblock.
     *
     * @return if we need an update packet
     */
    public boolean tick(Level world) {
        boolean needsPacket = false;
        for (ValveData data : valves) {
            data.activeTicks = Math.max(0, data.activeTicks - 1);
            if (data.activeTicks > 0 != data.prevActive) {
                needsPacket = true;
            }
            data.prevActive = data.activeTicks > 0;
        }
        return needsPacket;
    }

    protected double calculateAverageAmbientTemperature(Level world) {
        //Take a rough average of the biome temperature by calculating the average of all the corners of the multiblock
        BlockPos min = getMinPos();
        BlockPos max = getMaxPos();
        return HeatAPI.getAmbientTemp(getBiomeTemp(world,
              min,
              new BlockPos(max.getX(), min.getY(), min.getZ()),
              new BlockPos(min.getX(), min.getY(), max.getZ()),
              new BlockPos(max.getX(), min.getY(), max.getZ()),
              new BlockPos(min.getX(), max.getY(), min.getZ()),
              new BlockPos(max.getX(), max.getY(), min.getZ()),
              new BlockPos(min.getX(), max.getY(), max.getZ()),
              max
        ));
    }

    private static double getBiomeTemp(Level world, BlockPos... positions) {
        if (positions.length == 0) {
            throw new IllegalArgumentException("No positions given.");
        }
        return Arrays.stream(positions).mapToDouble(pos -> world.getBiome(pos).value().getTemperature(pos)).sum() / positions.length;
    }

    public boolean setShape(IShape shape) {
        if (shape instanceof VoxelCuboid cuboid) {
            bounds = cuboid;
            renderLocation = cuboid.getMinPos().relative(Direction.UP);
            setVolume(bounds.length() * bounds.width() * bounds.height());
            return true;
        }
        return false;
    }

    public void onCreated(Level world) {
        for (BlockPos pos : internalLocations) {
            BlockEntity tile = WorldUtils.getTileEntity(world, pos);
            if (tile instanceof IInternalMultiblock internalMultiblock) {
                internalMultiblock.setMultiblock(this);
            }
        }

        if (shouldCap(CacheSubstance.FLUID)) {
            for (IExtendedFluidTank tank : getFluidTanks(null)) {
                tank.setStackSize(Math.min(tank.getFluidAmount(), tank.getCapacity()), Action.EXECUTE);
            }
        }
        if (shouldCap(CacheSubstance.GAS)) {
            for (IGasTank tank : getGasTanks(null)) {
                tank.setStackSize(Math.min(tank.getStored(), tank.getCapacity()), Action.EXECUTE);
            }
        }
        if (shouldCap(CacheSubstance.INFUSION)) {
            for (IInfusionTank tank : getInfusionTanks(null)) {
                tank.setStackSize(Math.min(tank.getStored(), tank.getCapacity()), Action.EXECUTE);
            }
        }
        if (shouldCap(CacheSubstance.PIGMENT)) {
            for (IPigmentTank tank : getPigmentTanks(null)) {
                tank.setStackSize(Math.min(tank.getStored(), tank.getCapacity()), Action.EXECUTE);
            }
        }
        if (shouldCap(CacheSubstance.SLURRY)) {
            for (ISlurryTank tank : getSlurryTanks(null)) {
                tank.setStackSize(Math.min(tank.getStored(), tank.getCapacity()), Action.EXECUTE);
            }
        }
        if (shouldCap(CacheSubstance.ENERGY)) {
            for (IEnergyContainer container : getEnergyContainers(null)) {
                container.setEnergy(container.getEnergy().min(container.getMaxEnergy()));
            }
        }

        forceUpdateComparatorLevel();
    }

    protected boolean isRemote() {
        return remoteSupplier.getAsBoolean();
    }

    protected Level getWorld() {
        return worldSupplier.get();
    }

    protected boolean shouldCap(CacheSubstance<?, ?> type) {
        return true;
    }

    public void remove(Level world) {
        for (BlockPos pos : internalLocations) {
            BlockEntity tile = WorldUtils.getTileEntity(world, pos);
            if (tile instanceof IInternalMultiblock internalMultiblock) {
                internalMultiblock.setMultiblock(null);
            }
        }
        inventoryID = null;
        formed = false;
        recheckStructure = false;
    }

    public void meltdownHappened(Level world) {
    }

    public void readUpdateTag(CompoundTag tag) {
        NBTUtils.setIntIfPresent(tag, NBTConstants.VOLUME, this::setVolume);
        NBTUtils.setBlockPosIfPresent(tag, NBTConstants.RENDER_LOCATION, value -> renderLocation = value);
        bounds = new VoxelCuboid(NbtUtils.readBlockPos(tag.getCompound(NBTConstants.MIN)),
              NbtUtils.readBlockPos(tag.getCompound(NBTConstants.MAX)));
        NBTUtils.setUUIDIfPresentElse(tag, NBTConstants.INVENTORY_ID, value -> inventoryID = value, () -> inventoryID = null);
    }

    public void writeUpdateTag(CompoundTag tag) {
        tag.putInt(NBTConstants.VOLUME, getVolume());
        if (renderLocation != null) {//In theory this shouldn't be null here but check it anyway
            tag.put(NBTConstants.RENDER_LOCATION, NbtUtils.writeBlockPos(renderLocation));
        }
        tag.put(NBTConstants.MIN, NbtUtils.writeBlockPos(bounds.getMinPos()));
        tag.put(NBTConstants.MAX, NbtUtils.writeBlockPos(bounds.getMaxPos()));
        if (inventoryID != null) {
            tag.putUUID(NBTConstants.INVENTORY_ID, inventoryID);
        }
    }

    @ComputerMethod(nameOverride = "getLength")
    public int length() {
        return bounds.length();
    }

    @ComputerMethod(nameOverride = "getWidth")
    public int width() {
        return bounds.width();
    }

    @ComputerMethod(nameOverride = "getHeight")
    public int height() {
        return bounds.height();
    }

    @ComputerMethod
    public BlockPos getMinPos() {
        return bounds.getMinPos();
    }

    @ComputerMethod
    public BlockPos getMaxPos() {
        return bounds.getMaxPos();
    }

    public VoxelCuboid getBounds() {
        return bounds;
    }

    /**
     * Checks if this multiblock is formed and the given position is insides the bounds of this multiblock
     */
    public <T extends MultiblockData> boolean isPositionInsideBounds(@Nonnull Structure structure, @Nonnull BlockPos pos) {
        if (isFormed()) {
            CuboidRelative relativeLocation = getBounds().getRelativeLocation(pos);
            if (relativeLocation == CuboidRelative.INSIDE) {
                return true;
            } else if (relativeLocation.isWall()) {
                //If we are in the wall check if we are really an inner position. For example evap towers
                MultiblockManager<T> manager = (MultiblockManager<T>) structure.getManager();
                if (manager != null) {
                    IStructureValidator<T> validator = manager.createValidator();
                    if (validator instanceof CuboidStructureValidator<T> cuboidValidator) {
                        validator.init(getWorld(), manager, structure);
                        cuboidValidator.loadCuboid(getBounds());
                        return cuboidValidator.getStructureRequirement(pos) == StructureRequirement.INNER;
                    }
                }
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public List<IInventorySlot> getInventorySlots(@Nullable Direction side) {
        return isFormed() ? inventorySlots : Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<IExtendedFluidTank> getFluidTanks(@Nullable Direction side) {
        return isFormed() ? fluidTanks : Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<IGasTank> getGasTanks(@Nullable Direction side) {
        return isFormed() ? gasTanks : Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<IInfusionTank> getInfusionTanks(@Nullable Direction side) {
        return isFormed() ? infusionTanks : Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<IPigmentTank> getPigmentTanks(@Nullable Direction side) {
        return isFormed() ? pigmentTanks : Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<ISlurryTank> getSlurryTanks(@Nullable Direction side) {
        return isFormed() ? slurryTanks : Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<IEnergyContainer> getEnergyContainers(@Nullable Direction side) {
        return isFormed() ? energyContainers : Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<IHeatCapacitor> getHeatCapacitors(Direction side) {
        return isFormed() ? heatCapacitors : Collections.emptyList();
    }

    public Set<Direction> getDirectionsToEmit(BlockPos pos) {
        Set<Direction> directionsToEmit = EnumSet.noneOf(Direction.class);
        for (Direction direction : EnumUtils.DIRECTIONS) {
            BlockPos neighborPos = pos.relative(direction);
            if (!isKnownLocation(neighborPos)) {
                directionsToEmit.add(direction);
            }
        }
        return directionsToEmit;
    }

    public boolean isKnownLocation(BlockPos pos) {
        return locations.contains(pos) || internalLocations.contains(pos);
    }

    public Collection<ValveData> getValveData() {
        return valves;
    }

    @Override
    public void onContentsChanged() {
        markDirty();
    }

    @Override
    public int hashCode() {
        int code = 1;
        code = 31 * code + locations.hashCode();
        code = 31 * code + bounds.hashCode();
        code = 31 * code + getVolume();
        return code;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        MultiblockData data = (MultiblockData) obj;
        if (!data.locations.equals(locations)) {
            return false;
        }
        if (!data.bounds.equals(bounds)) {
            return false;
        }
        return data.getVolume() == getVolume();
    }

    public boolean isFormed() {
        return formed;
    }

    public void setFormedForce(boolean formed) {
        this.formed = formed;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    // Only call from the server
    public void markDirtyComparator(Level world) {
        if (!isFormed()) {
            return;
        }
        int newRedstoneLevel = getMultiblockRedstoneLevel();
        if (newRedstoneLevel != currentRedstoneLevel) {
            //Update the comparator value if it changed
            currentRedstoneLevel = newRedstoneLevel;
            //And inform all the valves that the level they should be supplying changed
            notifyAllUpdateComparator(world);
        }
    }

    public void notifyAllUpdateComparator(Level world) {
        for (ValveData valve : valves) {
            TileEntityMultiblock<?> tile = WorldUtils.getTileEntity(TileEntityMultiblock.class, world, valve.location);
            if (tile != null) {
                tile.markDirtyComparator();
            }
        }
    }

    public void forceUpdateComparatorLevel() {
        currentRedstoneLevel = getMultiblockRedstoneLevel();
    }

    protected int getMultiblockRedstoneLevel() {
        return 0;
    }

    public int getCurrentRedstoneLevel() {
        return currentRedstoneLevel;
    }
}