package mekanism.common.loot;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.common.loot.table.BaseBlockLootTables;
import mekanism.common.loot.table.BaseChestLootTables;
import mekanism.common.loot.table.BaseEntityLootTables;
import mekanism.common.loot.table.BaseFishingLootTables;
import mekanism.common.loot.table.BaseGiftLootTables;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTable.Builder;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public abstract class BaseLootProvider extends LootTableProvider {

    private final String modid;

    protected BaseLootProvider(DataGenerator gen, String modid) {
        super(gen);
        this.modid = modid;
    }

    @Nonnull
    @Override
    public String getName() {
        return super.getName() + ": " + modid;
    }

    @Nullable
    protected BaseBlockLootTables getBlockLootTable() {
        return null;
    }

    @Nullable
    protected BaseChestLootTables getChestLootTable() {
        return null;
    }

    @Nullable
    protected BaseEntityLootTables getEntityLootTable() {
        return null;
    }

    @Nullable
    protected BaseFishingLootTables getFishingLootTable() {
        return null;
    }

    @Nullable
    protected BaseGiftLootTables getGiftLootTable() {
        return null;
    }

    @Nonnull
    @Override
    protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, Builder>>>, LootContextParamSet>> getTables() {
        ImmutableList.Builder<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, Builder>>>, LootContextParamSet>> builder = new ImmutableList.Builder<>();
        BaseBlockLootTables blockLootTable = getBlockLootTable();
        if (blockLootTable != null) {
            builder.add(Pair.of(() -> blockLootTable, LootContextParamSets.BLOCK));
        }
        BaseChestLootTables chestLootTable = getChestLootTable();
        if (chestLootTable != null) {
            builder.add(Pair.of(() -> chestLootTable, LootContextParamSets.CHEST));
        }
        BaseEntityLootTables entityLootTable = getEntityLootTable();
        if (entityLootTable != null) {
            builder.add(Pair.of(() -> entityLootTable, LootContextParamSets.ENTITY));
        }
        BaseFishingLootTables fishingLootTable = getFishingLootTable();
        if (fishingLootTable != null) {
            builder.add(Pair.of(() -> fishingLootTable, LootContextParamSets.FISHING));
        }
        BaseGiftLootTables giftLootTable = getGiftLootTable();
        if (giftLootTable != null) {
            builder.add(Pair.of(() -> giftLootTable, LootContextParamSets.GIFT));
        }
        return builder.build();
    }

    @Override
    protected void validate(@Nonnull Map<ResourceLocation, LootTable> map, @Nonnull ValidationContext validationtracker) {
        //NO-OP, as we don't
    }
}