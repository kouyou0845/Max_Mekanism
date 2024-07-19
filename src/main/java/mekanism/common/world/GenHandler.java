package mekanism.common.world;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import mekanism.common.registration.impl.SetupFeatureDeferredRegister.MekFeature;
import mekanism.common.registration.impl.SetupFeatureRegistryObject;
import mekanism.common.registries.MekanismFeatures;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.BiomeCategory;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.event.world.BiomeLoadingEvent;

public class GenHandler {

    private GenHandler() {
    }

    public static void onBiomeLoad(BiomeLoadingEvent event) {
        if (isValidBiome(event.getCategory())) {
            BiomeGenerationSettingsBuilder generation = event.getGeneration();
            //Add ores
            for (MekFeature<ResizableOreFeatureConfig, ResizableOreFeature>[] features : MekanismFeatures.ORES.values()) {
                for (MekFeature<ResizableOreFeatureConfig, ResizableOreFeature> feature : features) {
                    generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, feature.placedFeature());
                }
            }
            //Add salt
            generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MekanismFeatures.SALT.placedFeature());
        }
    }

    private static boolean isValidBiome(Biome.BiomeCategory biomeCategory) {
        //If this does weird things to unclassified biomes (Category.NONE), then we should also mark that biome as invalid
        return biomeCategory != BiomeCategory.THEEND && biomeCategory != BiomeCategory.NETHER;
    }

    /**
     * @return {@code true} if some retro-generation happened.
     *
     * @apiNote Only call this method if the chunk at the given position is loaded.
     * @implNote Adapted from {@link ChunkGenerator#applyBiomeDecoration(WorldGenLevel, ChunkAccess, StructureFeatureManager)}.
     */
    public static boolean generate(ServerLevel world, ChunkPos chunkPos) {
        boolean generated = false;
        if (!SharedConstants.debugVoidTerrain(chunkPos)) {
            SectionPos sectionPos = SectionPos.of(chunkPos, world.getMinSection());
            BlockPos blockPos = sectionPos.origin();
            ChunkGenerator chunkGenerator = world.getChunkSource().getGenerator();
            WorldgenRandom random = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.seedUniquifier()));
            long decorationSeed = random.setDecorationSeed(world.getSeed(), blockPos.getX(), blockPos.getZ());
            int decorationStep = GenerationStep.Decoration.UNDERGROUND_ORES.ordinal() - 1;
            ToIntFunction<PlacedFeature> featureIndex;
            //Note: We use the runtime biome source instead of the actual biome source as that is all we have access to
            // and the only case in vanilla where it actually seems like it might make a difference is for super-flat
            // worlds which don't really have any generation to begin with. If this ends up causing issues in any modded
            // dimensions, then it might be worth ATing to access the actual biomeSource variable
            BiomeSource biomeSource = chunkGenerator.getBiomeSource();
            List<BiomeSource.StepFeatureData> list = biomeSource.featuresPerStep();
            if (decorationStep < list.size()) {
                //Use the feature index lookup mapping. We can skip a lot of vanilla's logic here that is needed
                // for purposes of getting all the features we want to be doing, as we know which features we want
                // to generate and only lookup those. We also don't need to worry about if the biome can actually
                // support our feature as that is validated via the placement context and allows us to drastically
                // cut down on calculating it here
                featureIndex = list.get(decorationStep).indexMapping();
            } else {
                featureIndex = feature -> -1;
            }
            for (MekFeature<ResizableOreFeatureConfig, ResizableOreFeature>[] features : MekanismFeatures.ORES.values()) {
                for (MekFeature<ResizableOreFeatureConfig, ResizableOreFeature> feature : features) {
                    generated |= place(world, chunkGenerator, blockPos, random, decorationSeed, decorationStep, featureIndex, feature);
                }
            }
            generated |= place(world, chunkGenerator, blockPos, random, decorationSeed, decorationStep, featureIndex, MekanismFeatures.SALT);
            world.setCurrentlyGenerating(null);
        }
        return generated;
    }

    private static boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, BlockPos blockPos, WorldgenRandom random,
          long decorationSeed, int decorationStep, ToIntFunction<PlacedFeature> featureIndex, MekFeature<?, ?> feature) {
        SetupFeatureRegistryObject<?, ?> retrogen = feature.retrogen();
        PlacedFeature baseFeature = feature.feature().getPlacedFeature();
        //Check the index of the source feature instead of the retrogen feature
        random.setFeatureSeed(decorationSeed, featureIndex.applyAsInt(baseFeature), decorationStep);
        world.setCurrentlyGenerating(() -> retrogen.getPlacedFeatureKey().toString());
        //Note: We call placeWithContext directly to allow for doing a placeWithBiomeCheck, except by having the context pretend
        // it is the non retrogen feature which actually is added to the various biomes
        return retrogen.getPlacedFeature().placeWithContext(new PlacementContext(world, chunkGenerator, Optional.of(baseFeature)), random, blockPos);
    }
}