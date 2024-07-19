package mekanism.common.loot.table;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import mekanism.api.providers.IEntityTypeProvider;
import net.minecraft.data.loot.EntityLoot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootTable;

public abstract class BaseEntityLootTables extends EntityLoot {

    private final Set<EntityType<?>> knownEntityTypes = new ObjectOpenHashSet<>();

    @Override
    protected abstract void addTables();

    @Override
    protected void add(@Nonnull EntityType<?> type, @Nonnull LootTable.Builder table) {
        //Overwrite the core register method to add to our list of known entity types
        //Note: This isn't the actual core method as that one takes a ResourceLocation, but all our things wil pass through this one
        super.add(type, table);
        knownEntityTypes.add(type);
    }

    @Nonnull
    @Override
    protected Iterable<EntityType<?>> getKnownEntities() {
        return knownEntityTypes;
    }

    protected void add(@Nonnull IEntityTypeProvider typeProvider, @Nonnull LootTable.Builder table) {
        add(typeProvider.getEntityType(), table);
    }
}