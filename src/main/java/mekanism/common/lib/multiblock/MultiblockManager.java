package mekanism.common.lib.multiblock;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import mekanism.api.Coord4D;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import mekanism.common.util.WorldUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MultiblockManager<T extends MultiblockData> {

    private static final Set<MultiblockManager<?>> managers = new ObjectOpenHashSet<>();

    private final String name;
    private final String nameLower;

    private final Supplier<MultiblockCache<T>> cacheSupplier;
    private final Supplier<IStructureValidator<T>> validatorSupplier;

    /**
     * A map containing references to all multiblock inventory caches.
     */
    public final Map<UUID, CacheWrapper> inventories = new Object2ObjectOpenHashMap<>();

    public MultiblockManager(String name, Supplier<MultiblockCache<T>> cacheSupplier, Supplier<IStructureValidator<T>> validatorSupplier) {
        this.name = name;
        this.nameLower = name.toLowerCase(Locale.ROOT);
        this.cacheSupplier = cacheSupplier;
        this.validatorSupplier = validatorSupplier;
        managers.add(this);
    }

    public MultiblockCache<T> createCache() {
        return cacheSupplier.get();
    }

    public IStructureValidator<T> createValidator() {
        return validatorSupplier.get();
    }

    public String getName() {
        return name;
    }

    public String getNameLower() {
        return nameLower;
    }

    @Nullable
    public static UUID getMultiblockID(TileEntityMultiblock<?> tile) {
        return tile.getMultiblock().inventoryID;
    }

    public boolean isCompatible(BlockEntity tile) {
        if (tile instanceof IMultiblock<?> multiblock) {
            return multiblock.getManager() == this;
        }
        return false;
    }

    public static void reset() {
        for (MultiblockManager<?> manager : managers) {
            manager.inventories.clear();
        }
    }

    public void invalidate(IMultiblock<?> multiblock) {
        CacheWrapper cache = inventories.get(multiblock.getCacheID());
        if (cache != null) {
            cache.locations.remove(multiblock.getTileCoord());
            if (cache.locations.isEmpty()) {
                inventories.remove(multiblock.getCacheID());
            }
        }
    }

    /**
     * Grabs an inventory from the world's caches, and removes all the world's references to it. NOTE: this is not guaranteed to remove all references if somehow blocks
     * with this inventory ID exist in unloaded chunks when the inventory is pulled. We should consider whether we should implement a way to mitigate this.
     *
     * @param world - world the cache is stored in
     * @param id    - inventory ID to pull
     *
     * @return correct multiblock inventory cache
     */
    public MultiblockCache<T> pullInventory(Level world, UUID id) {
        CacheWrapper toReturn = inventories.get(id);
        for (Coord4D obj : toReturn.locations) {
            BlockEntity tile = WorldUtils.getTileEntity(BlockEntity.class, world, obj.getPos());
            if (tile instanceof IMultiblock<?> multiblock) {
                multiblock.resetCache();
            }
        }
        inventories.remove(id);
        return toReturn.getCache();
    }

    /**
     * Grabs a unique inventory ID for a multiblock.
     *
     * @return unique inventory ID
     */
    public UUID getUniqueInventoryID() {
        return UUID.randomUUID();
    }

    public void updateCache(IMultiblock<T> tile, T multiblock) {
        inventories.computeIfAbsent(tile.getCacheID(), id -> new CacheWrapper()).update(tile, multiblock);
    }

    public class CacheWrapper {

        private MultiblockCache<T> cache;
        private final Set<Coord4D> locations = new ObjectOpenHashSet<>();

        private CacheWrapper() {
        }

        public MultiblockCache<T> getCache() {
            return cache;
        }

        public void update(IMultiblock<T> tile, T multiblock) {
            if (multiblock.isFormed()) {
                if (tile.isMaster()) {
                    // create a new cache for the tile if it needs one
                    if (!tile.hasCache()) {
                        tile.setCache(createCache());
                        locations.add(tile.getTileCoord());
                    } else if (cache != tile.getCache()) {
                        locations.add(tile.getTileCoord());
                    }
                    // if this is the master tile, sync the cache with the multiblock and then update our reference
                    tile.getCache().sync(multiblock);
                    cache = tile.getCache();
                }
            } else if (tile.hasCache()) {
                if (cache != tile.getCache()) {
                    // if the tile doesn't have a formed multiblock but has a cache, update our reference
                    cache = tile.getCache();
                    locations.add(tile.getTileCoord());
                }
            } else if (cache != null) {
                // if the tile doesn't have a cache, but we do, update the tile's reference
                tile.setCache(cache);
                locations.add(tile.getTileCoord());
            }
        }
    }
}