package mekanism.common.lib.radiation;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.api.Chunk3D;
import mekanism.api.Coord4D;
import mekanism.api.MekanismAPI;
import mekanism.api.NBTConstants;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.chemical.gas.attribute.GasAttributes.Radiation;
import mekanism.api.radiation.IRadiationManager;
import mekanism.api.radiation.IRadiationSource;
import mekanism.api.radiation.capability.IRadiationEntity;
import mekanism.api.radiation.capability.IRadiationShielding;
import mekanism.api.text.EnumColor;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.lib.collection.HashList;
import mekanism.common.network.to_client.PacketRadiationData;
import mekanism.common.registries.MekanismDamageSource;
import mekanism.common.registries.MekanismParticleTypes;
import mekanism.common.registries.MekanismSounds;
import mekanism.common.util.CapabilityUtils;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * The RadiationManager handles radiation across all in-game dimensions. Radiation exposure levels are provided in _sieverts, defining a rate of accumulation of
 * equivalent dose. For reference, here are examples of equivalent dose (credit: wikipedia)
 * <ul>
 * <li>100 nSv: baseline dose (banana equivalent dose)</li>
 * <li>250 nSv: airport security screening</li>
 * <li>1 mSv: annual total civilian dose equivalent</li>
 * <li>50 mSv: annual total occupational equivalent dose limit</li>
 * <li>250 mSv: total dose equivalent from 6-month trip to mars</li>
 * <li>1 Sv: maximum allowed dose allowed for NASA astronauts over their careers</li>
 * <li>5 Sv: dose required to (50% chance) kill human if received over 30-day period</li>
 * <li>50 Sv: dose received after spending 10 min next to Chernobyl reactor core directly after meltdown</li>
 * </ul>
 * For defining rate of accumulation, we use _sieverts per hour_ (Sv/h). Here are examples of dose accumulation rates.
 * <ul>
 * <li>100 nSv/h: max recommended human irradiation</li>
 * <li>2.7 uSv/h: irradiation from airline at cruise altitude</li>
 * <li>190 mSv/h: highest reading from fallout of Trinity (Manhattan project test) bomb, _20 miles away_, 3 hours after detonation</li>
 * <li>~500 Sv/h: irradiation inside primary containment vessel of Fukushima power station (at this rate, it takes 30 seconds to accumulate a median lethal dose)</li>
 * </ul>
 *
 * @author aidancbrady
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RadiationManager implements IRadiationManager {

    /**
     * RadiationManager for handling radiation across all dimensions
     */
    public static final RadiationManager INSTANCE = new RadiationManager();
    private static final String DATA_HANDLER_NAME = "radiation_manager";
    private static final IntSupplier MAX_RANGE = () -> MekanismConfig.general.radiationChunkCheckRadius.get() * 16;
    private static final Random RAND = new Random();

    public static final double BASELINE = 0.0000001; // 100 nSv/h
    public static final double MIN_MAGNITUDE = 0.00001; // 10 uSv/h

    private boolean loaded;

    private final Table<Chunk3D, Coord4D, RadiationSource> radiationTable = HashBasedTable.create();
    private final Table<Chunk3D, Coord4D, IRadiationSource> radiationView = Tables.unmodifiableTable(radiationTable);
    private final Map<ResourceLocation, List<Meltdown>> meltdowns = new Object2ObjectOpenHashMap<>();

    private final Object2DoubleMap<UUID> playerExposureMap = new Object2DoubleOpenHashMap<>();

    // client fields
    private RadiationScale clientRadiationScale = RadiationScale.NONE;
    private double clientEnvironmentalRadiation = BASELINE;

    /**
     * Note: This can and will be null on the client side
     */
    @Nullable
    private RadiationDataHandler dataHandler;

    private RadiationManager() {
    }

    @Override
    public boolean isRadiationEnabled() {
        return MekanismConfig.general.radiationEnabled.get();
    }

    private void markDirty() {
        if (dataHandler != null) {
            dataHandler.setDirty();
        }
    }

    @Override
    public DamageSource getRadiationDamageSource() {
        return MekanismDamageSource.RADIATION;
    }

    @Override
    public double getRadiationLevel(Entity entity) {
        return getRadiationLevel(new Coord4D(entity));
    }

    @Override
    public Table<Chunk3D, Coord4D, IRadiationSource> getRadiationSources() {
        return radiationView;
    }

    @Override
    public void removeRadiationSources(Chunk3D chunk) {
        Map<Coord4D, RadiationSource> chunkSources = radiationTable.row(chunk);
        if (!chunkSources.isEmpty()) {
            chunkSources.clear();
            markDirty();
            updateClientRadiationForAll(chunk.dimension);
        }
    }

    @Override
    public void removeRadiationSource(Coord4D coord) {
        Chunk3D chunk = new Chunk3D(coord);
        if (radiationTable.contains(chunk, coord)) {
            radiationTable.remove(chunk, coord);
            markDirty();
            updateClientRadiationForAll(coord.dimension);
        }
    }

    @Override
    public double getRadiationLevel(Coord4D coord) {
        Set<Chunk3D> checkChunks = new Chunk3D(coord).expand(MekanismConfig.general.radiationChunkCheckRadius.get());
        double level = BASELINE;
        for (Chunk3D chunk : checkChunks) {
            for (Map.Entry<Coord4D, RadiationSource> entry : radiationTable.row(chunk).entrySet()) {
                // we only compute exposure when within the MAX_RANGE bounds
                if (entry.getKey().distanceTo(coord) <= MAX_RANGE.getAsInt()) {
                    level += computeExposure(coord, entry.getValue());
                }
            }
        }
        return level;
    }

    @Override
    public void radiate(Coord4D coord, double magnitude) {
        if (!isRadiationEnabled()) {
            return;
        }
        Map<Coord4D, RadiationSource> radiationSourceMap = radiationTable.row(new Chunk3D(coord));
        RadiationSource src = radiationSourceMap.get(coord);
        if (src == null) {
            radiationSourceMap.put(coord, new RadiationSource(coord, magnitude));
        } else {
            src.radiate(magnitude);
        }
        markDirty();
        //Update radiation levels immediately
        updateClientRadiationForAll(coord.dimension);
    }

    @Override
    public void radiate(LivingEntity entity, double magnitude) {
        if (!isRadiationEnabled()) {
            return;
        }
        if (!(entity instanceof Player player) || MekanismUtils.isPlayingMode(player)) {
            entity.getCapability(Capabilities.RADIATION_ENTITY_CAPABILITY).ifPresent(c -> c.radiate(magnitude * (1 - Math.min(1, getRadiationResistance(entity)))));
        }
    }

    @Override
    public void dumpRadiation(Coord4D coord, IGasHandler gasHandler, boolean clearRadioactive) {
        for (int tank = 0, gasTanks = gasHandler.getTanks(); tank < gasTanks; tank++) {
            if (dumpRadiation(coord, gasHandler.getChemicalInTank(tank)) && clearRadioactive) {
                gasHandler.setChemicalInTank(tank, GasStack.EMPTY);
            }
        }
    }

    @Override
    public void dumpRadiation(Coord4D coord, List<IGasTank> gasTanks, boolean clearRadioactive) {
        for (IGasTank gasTank : gasTanks) {
            if (dumpRadiation(coord, gasTank.getStack()) && clearRadioactive) {
                gasTank.setEmpty();
            }
        }
    }

    @Override
    public boolean dumpRadiation(Coord4D coord, GasStack stack) {
        if (!stack.isEmpty() && stack.has(Radiation.class)) {
            double radioactivity = stack.get(Radiation.class).getRadioactivity();
            radiate(coord, radioactivity * stack.getAmount());
            return true;
        }
        return false;
    }

    public void createMeltdown(Level world, BlockPos minPos, BlockPos maxPos, double magnitude, double chance, UUID multiblockID) {
        meltdowns.computeIfAbsent(world.dimension().location(), id -> new ArrayList<>()).add(new Meltdown(minPos, maxPos, magnitude, chance, multiblockID));
        markDirty();
    }

    public void clearSources() {
        if (!radiationTable.isEmpty()) {
            radiationTable.clear();
            markDirty();
            updateClientRadiationForAll(player -> true);
        }
    }

    private double computeExposure(Coord4D coord, RadiationSource source) {
        return source.getMagnitude() / Math.max(1, coord.distanceToSquared(source.getPos()));
    }

    private double getRadiationResistance(LivingEntity entity) {
        double resistance = 0;
        for (EquipmentSlot type : EnumUtils.ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(type);
            Optional<IRadiationShielding> shielding = CapabilityUtils.getCapability(stack, Capabilities.RADIATION_SHIELDING_CAPABILITY, null).resolve();
            if (shielding.isPresent()) {
                resistance += shielding.get().getRadiationShielding();
            }
        }
        return resistance;
    }

    private void updateClientRadiationForAll(ResourceKey<Level> dimension) {
        updateClientRadiationForAll(player -> player.getLevel().dimension() == dimension);
    }

    private void updateClientRadiationForAll(Predicate<ServerPlayer> clearForPlayer) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            //Validate it is not null in case we somehow are being called from the client or at some other unexpected time
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (clearForPlayer.test(player)) {
                    updateClientRadiation(player);
                }
            }
        }
    }

    public void updateClientRadiation(ServerPlayer player) {
        double magnitude = getRadiationLevel(player);
        double scaledMagnitude = Math.ceil(magnitude / BASELINE);
        //If the last sync radiation value is different in magnitude by over the baseline, sync
        // Note: If it is not present this will always be marked as needing a sync as it is not possible for scaledMagnitude
        // to be zero as magnitude will always be at least BASELINE
        if (scaledMagnitude != playerExposureMap.getOrDefault(player.getUUID(), 0)) {
            playerExposureMap.put(player.getUUID(), scaledMagnitude);
            Mekanism.packetHandler().sendTo(PacketRadiationData.createEnvironmental(magnitude), player);
        }
    }

    public void setClientEnvironmentalRadiation(double radiation) {
        clientEnvironmentalRadiation = radiation;
        clientRadiationScale = RadiationScale.get(clientEnvironmentalRadiation);
    }

    public double getClientEnvironmentalRadiation() {
        return isRadiationEnabled() ? clientEnvironmentalRadiation : BASELINE;
    }

    public RadiationScale getClientScale() {
        return isRadiationEnabled() ? clientRadiationScale : RadiationScale.NONE;
    }

    public void tickClient(Player player) {
        // terminate early if we're disabled
        if (!isRadiationEnabled()) {
            return;
        }
        // perhaps also play Geiger counter sound effect, even when not using item (similar to fallout)
        if (clientRadiationScale != RadiationScale.NONE && player.level.getRandom().nextInt(2) == 0) {
            int count = player.level.getRandom().nextInt(clientRadiationScale.ordinal() * MekanismConfig.client.radiationParticleCount.get());
            int radius = MekanismConfig.client.radiationParticleRadius.get();
            for (int i = 0; i < count; i++) {
                double x = player.getX() + player.level.getRandom().nextDouble() * radius * 2 - radius;
                double y = player.getY() + player.level.getRandom().nextDouble() * radius * 2 - radius;
                double z = player.getZ() + player.level.getRandom().nextDouble() * radius * 2 - radius;
                player.level.addParticle(MekanismParticleTypes.RADIATION.get(), x, y, z, 0, 0, 0);
            }
        }
    }

    public void tickServer(ServerPlayer player) {
        updateEntityRadiation(player);
    }

    private void updateEntityRadiation(LivingEntity entity) {
        // terminate early if we're disabled
        if (!isRadiationEnabled()) {
            return;
        }
        LazyOptional<IRadiationEntity> radiationCap = entity.getCapability(Capabilities.RADIATION_ENTITY_CAPABILITY);
        // each tick, there is a 1/20 chance we will apply radiation to each player
        // this helps distribute the CPU load across ticks, and makes exposure slightly inconsistent
        if (entity.level.getRandom().nextInt(20) == 0) {
            double magnitude = getRadiationLevel(entity);
            if (magnitude > BASELINE && (!(entity instanceof Player player) || MekanismUtils.isPlayingMode(player))) {
                // apply radiation to the player
                radiate(entity, magnitude / 3_600D); // convert to Sv/s
            }
            radiationCap.ifPresent(IRadiationEntity::decay);
        }
        // update the radiation capability (decay, sync, effects)
        radiationCap.ifPresent(c -> c.update(entity));
    }

    public void tickServerWorld(Level world) {
        // terminate early if we're disabled
        if (!isRadiationEnabled()) {
            return;
        }
        if (!loaded) {
            createOrLoad();
        }

        // update meltdowns
        List<Meltdown> dimensionMeltdowns = meltdowns.getOrDefault(world.dimension().location(), Collections.emptyList());
        if (!dimensionMeltdowns.isEmpty()) {
            dimensionMeltdowns.removeIf(meltdown -> meltdown.update(world));
            //If we have/had any meltdowns mark our data handler as dirty as when a meltdown updates
            // the number of ticks it has been around for will change
            markDirty();
        }
    }

    public void tickServer() {
        // terminate early if we're disabled
        if (!isRadiationEnabled()) {
            return;
        }
        // each tick, there's a 1/20 chance we'll decay radiation sources (averages to 1 decay operation per second)
        if (RAND.nextInt(20) == 0) {
            Collection<RadiationSource> sources = radiationTable.values();
            if (!sources.isEmpty()) {
                // remove if source gets too low
                sources.removeIf(RadiationSource::decay);
                //Mark dirty regardless if we have any sources as magnitude changes or radiation sources change
                markDirty();
                //Update radiation levels for any players where it has changed
                updateClientRadiationForAll(player -> true);
            }
        }
    }

    /**
     * Note: This should only be called from the server side
     */
    public void createOrLoad() {
        if (dataHandler == null) {
            //Always associate the world with the over world as the frequencies are global
            DimensionDataStorage savedData = ServerLifecycleHooks.getCurrentServer().overworld().getDataStorage();
            dataHandler = savedData.computeIfAbsent(tag -> {
                RadiationDataHandler handler = new RadiationDataHandler();
                handler.load(tag);
                return handler;
            }, RadiationDataHandler::new, DATA_HANDLER_NAME);
            dataHandler.setManagerAndSync(this);
            dataHandler.clearCached();
        }

        loaded = true;
    }

    public void reset() {
        //Clear the table directly instead of via the method, so it doesn't mark it as dirty
        radiationTable.clear();
        playerExposureMap.clear();
        meltdowns.clear();
        dataHandler = null;
        loaded = false;
    }

    public void resetClient() {
        clientRadiationScale = RadiationScale.NONE;
        clientEnvironmentalRadiation = BASELINE;
    }

    public void resetPlayer(UUID uuid) {
        playerExposureMap.removeDouble(uuid);
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        Level world = event.getEntityLiving().getCommandSenderWorld();
        if (!world.isClientSide() && !(event.getEntityLiving() instanceof Player)) {
            updateEntityRadiation(event.getEntityLiving());
        }
    }

    public enum RadiationScale {
        NONE,
        LOW,
        MEDIUM,
        ELEVATED,
        HIGH,
        EXTREME;

        /**
         * Get the corresponding RadiationScale from an equivalent dose rate (Sv/h)
         */
        public static RadiationScale get(double magnitude) {
            if (magnitude < 0.00001) { // 10 uSv/h
                return NONE;
            } else if (magnitude < 0.001) { // 1 mSv/h
                return LOW;
            } else if (magnitude < 0.1) { // 100 mSv/h
                return MEDIUM;
            } else if (magnitude < 10) { // 100 Sv/h
                return ELEVATED;
            } else if (magnitude < 100) {
                return HIGH;
            }
            return EXTREME;
        }

        /**
         * For both Sv and Sv/h.
         */
        public static EnumColor getSeverityColor(double magnitude) {
            if (magnitude <= BASELINE) {
                return EnumColor.BRIGHT_GREEN;
            } else if (magnitude < 0.00001) { // 10 uSv/h
                return EnumColor.GRAY;
            } else if (magnitude < 0.001) { // 1 mSv/h
                return EnumColor.YELLOW;
            } else if (magnitude < 0.1) { // 100 mSv/h
                return EnumColor.ORANGE;
            } else if (magnitude < 10) { // 100 Sv/h
                return EnumColor.RED;
            }
            return EnumColor.DARK_RED;
        }

        private static final double LOG_BASELINE = Math.log10(MIN_MAGNITUDE);
        private static final double LOG_MAX = Math.log10(100); // 100 Sv
        private static final double SCALE = LOG_MAX - LOG_BASELINE;

        /**
         * Gets the severity of a dose (between 0 and 1) from a provided dosage in Sv.
         */
        public static double getScaledDoseSeverity(double magnitude) {
            if (magnitude < MIN_MAGNITUDE) {
                return 0;
            }
            return Math.min(1, Math.max(0, (-LOG_BASELINE + Math.log10(magnitude)) / SCALE));
        }

        public SoundEvent getSoundEvent() {
            return switch (this) {
                case LOW -> MekanismSounds.GEIGER_SLOW.get();
                case MEDIUM -> MekanismSounds.GEIGER_MEDIUM.get();
                case ELEVATED, HIGH -> MekanismSounds.GEIGER_ELEVATED.get();
                case EXTREME -> MekanismSounds.GEIGER_FAST.get();
                default -> null;
            };
        }
    }

    public static class RadiationDataHandler extends SavedData {

        private Map<ResourceLocation, List<Meltdown>> savedMeltdowns = Collections.emptyMap();
        public List<RadiationSource> loadedSources = Collections.emptyList();
        public RadiationManager manager;

        public void setManagerAndSync(RadiationManager m) {
            manager = m;
            // don't sync the manager if radiation has been disabled
            if (MekanismAPI.getRadiationManager().isRadiationEnabled()) {
                for (RadiationSource source : loadedSources) {
                    manager.radiationTable.put(new Chunk3D(source.getPos()), source.getPos(), source);
                }
                for (Map.Entry<ResourceLocation, List<Meltdown>> entry : savedMeltdowns.entrySet()) {
                    List<Meltdown> meltdowns = entry.getValue();
                    manager.meltdowns.computeIfAbsent(entry.getKey(), id -> new ArrayList<>(meltdowns.size())).addAll(meltdowns);
                }
            }
        }

        public void clearCached() {
            //Clear cached sources and meltdowns after loading them to not keep pointers in our data handler
            // that are referencing objects that eventually will be removed
            loadedSources = Collections.emptyList();
            savedMeltdowns = Collections.emptyMap();
        }

        public void load(@Nonnull CompoundTag nbtTags) {
            if (nbtTags.contains(NBTConstants.RADIATION_LIST, Tag.TAG_LIST)) {
                ListTag list = nbtTags.getList(NBTConstants.RADIATION_LIST, Tag.TAG_COMPOUND);
                loadedSources = new HashList<>(list.size());
                for (Tag nbt : list) {
                    loadedSources.add(RadiationSource.load((CompoundTag) nbt));
                }
            } else {
                loadedSources = Collections.emptyList();
            }
            if (nbtTags.contains(NBTConstants.MELTDOWNS, Tag.TAG_COMPOUND)) {
                CompoundTag meltdownNBT = nbtTags.getCompound(NBTConstants.MELTDOWNS);
                savedMeltdowns = new HashMap<>(meltdownNBT.size());
                for (String dim : meltdownNBT.getAllKeys()) {
                    ResourceLocation dimension = ResourceLocation.tryParse(dim);
                    if (dimension != null) {
                        //It should be a valid dimension, but validate it just in case
                        ListTag meltdowns = meltdownNBT.getList(dim, Tag.TAG_COMPOUND);
                        savedMeltdowns.put(dimension, meltdowns.stream().map(nbt -> Meltdown.load((CompoundTag) nbt)).collect(Collectors.toList()));
                    }
                }
            } else {
                savedMeltdowns = Collections.emptyMap();
            }
        }

        @Nonnull
        @Override
        public CompoundTag save(@Nonnull CompoundTag nbtTags) {
            if (!manager.radiationTable.isEmpty()) {
                ListTag list = new ListTag();
                for (RadiationSource source : manager.radiationTable.values()) {
                    CompoundTag compound = new CompoundTag();
                    source.write(compound);
                    list.add(compound);
                }
                nbtTags.put(NBTConstants.RADIATION_LIST, list);
            }
            if (!manager.meltdowns.isEmpty()) {
                CompoundTag meltdownNBT = new CompoundTag();
                for (Map.Entry<ResourceLocation, List<Meltdown>> entry : manager.meltdowns.entrySet()) {
                    List<Meltdown> meltdowns = entry.getValue();
                    if (!meltdowns.isEmpty()) {
                        ListTag list = new ListTag();
                        for (Meltdown meltdown : meltdowns) {
                            CompoundTag compound = new CompoundTag();
                            meltdown.write(compound);
                            list.add(compound);
                        }
                        meltdownNBT.put(entry.getKey().toString(), list);
                    }
                }
                if (!meltdownNBT.isEmpty()) {
                    nbtTags.put(NBTConstants.MELTDOWNS, meltdownNBT);
                }
            }
            return nbtTags;
        }
    }
}
