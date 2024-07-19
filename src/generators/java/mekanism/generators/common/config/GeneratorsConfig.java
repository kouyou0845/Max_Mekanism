package mekanism.generators.common.config;

import mekanism.api.math.FloatingLong;
import mekanism.common.config.BaseMekanismConfig;
import mekanism.common.config.value.CachedBooleanValue;
import mekanism.common.config.value.CachedDoubleValue;
import mekanism.common.config.value.CachedFloatingLongValue;
import mekanism.common.config.value.CachedIntValue;
import mekanism.common.config.value.CachedLongValue;
import mekanism.common.config.value.CachedResourceLocationListValue;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import mekanism.generators.common.tile.TileEntityHeatGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig.Type;

public class GeneratorsConfig extends BaseMekanismConfig {

    private static final String TURBINE_CATEGORY = "turbine";
    private static final String WIND_CATEGORY = "wind_generator";
    private static final String HEAT_CATEGORY = "heat_generator";
    private static final String HOHLRAUM_CATEGORY = "hohlraum";
    private static final String FUSION_CATEGORY = "fusion_reactor";
    private static final String FISSION_CATEGORY = "fission_reactor";

    private final ForgeConfigSpec configSpec;

    public final CachedFloatingLongValue advancedSolarGeneration;
    public final CachedFloatingLongValue bioGeneration;
    public final CachedFloatingLongValue heatGeneration;
    public final CachedFloatingLongValue heatGenerationLava;
    public final CachedFloatingLongValue heatGenerationNether;
    public final CachedIntValue heatGenerationFluidRate;
    public final CachedFloatingLongValue solarGeneration;
    public final CachedIntValue turbineBladesPerCoil;
    public final CachedDoubleValue turbineVentGasFlow;
    public final CachedDoubleValue turbineDisperserGasFlow;
    public final CachedIntValue condenserRate;
    public final CachedFloatingLongValue energyPerFusionFuel;
    public final CachedFloatingLongValue windGenerationMin;
    public final CachedFloatingLongValue windGenerationMax;
    public final CachedIntValue windGenerationMinY;
    public final CachedIntValue windGenerationMaxY;
    public final CachedResourceLocationListValue windGenerationDimBlacklist;

    public final CachedFloatingLongValue energyPerFissionFuel;
    public final CachedDoubleValue fissionCasingHeatCapacity;
    public final CachedDoubleValue fissionSurfaceAreaTarget;
    public final CachedBooleanValue fissionMeltdownsEnabled;
    public final CachedDoubleValue fissionMeltdownChance;
    public final CachedDoubleValue fissionMeltdownRadiationMultiplier;
    public final CachedDoubleValue fissionPostMeltdownDamage;
    public final CachedDoubleValue defaultBurnRate;
    public final CachedLongValue burnPerAssembly;

    public final CachedLongValue hohlraumMaxGas;
    public final CachedLongValue hohlraumFillRate;

    public final CachedDoubleValue fusionThermocoupleEfficiency;
    public final CachedDoubleValue fusionCasingThermalConductivity;
    public final CachedDoubleValue fusionWaterHeatingRatio;

    GeneratorsConfig() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Mekanism Generators Config. This config is synced between server and client.").push("generators");

        bioGeneration = CachedFloatingLongValue.define(this, builder, "Amount of energy in Joules the Bio Generator produces per tick.",
              "bioGeneration", FloatingLong.createConst(350));
        energyPerFusionFuel = CachedFloatingLongValue.define(this, builder, "Affects the Injection Rate, Max Temp, and Ignition Temp.",
              "energyPerFusionFuel", FloatingLong.createConst(10_000_000));
        solarGeneration = CachedFloatingLongValue.define(this, builder, "Peak output for the Solar Generator. Note: It can go higher than this value in some extreme environments.",
              "solarGeneration", FloatingLong.createConst(50));
        advancedSolarGeneration = CachedFloatingLongValue.define(this, builder, "Peak output for the Advanced Solar Generator. Note: It can go higher than this value in some extreme environments.",
              "advancedSolarGeneration", FloatingLong.createConst(300));

        builder.comment("Heat Generator Settings").push(HEAT_CATEGORY);
        heatGeneration = CachedFloatingLongValue.define(this, builder, "Amount of energy in Joules the Heat Generator produces per tick. heatGeneration + heatGenerationLava * lavaSides + heatGenerationNether. Note: lavaSides is how many sides are adjacent to lava, this includes the block itself if it is lava logged allowing for a max of 7 \"sides\".",
              "heatGeneration", FloatingLong.createConst(200));
        heatGenerationLava = CachedFloatingLongValue.define(this, builder, "Multiplier of effectiveness of Lava that is adjacent to the Heat Generator.",
              "heatGenerationLava", FloatingLong.createConst(30));
        heatGenerationNether = CachedFloatingLongValue.define(this, builder, "Add this amount of Joules to the energy produced by a heat generator if it is in an 'ultrawarm' dimension, in vanilla this is just the Nether.",
              "heatGenerationNether", FloatingLong.createConst(100));
        heatGenerationFluidRate = CachedIntValue.wrap(this, builder.comment("The amount of lava in mB that gets consumed to transfer heatGeneration Joules to the Heat Generator.")
              .defineInRange("heatGenerationFluidRate", 10, 1, TileEntityHeatGenerator.MAX_FLUID));
        builder.pop();

        builder.comment("Turbine Settings").push(TURBINE_CATEGORY);
        turbineBladesPerCoil = CachedIntValue.wrap(this, builder.comment("The number of blades on each turbine coil per blade applied.")
              .define("turbineBladesPerCoil", 4));
        turbineVentGasFlow = CachedDoubleValue.wrap(this, builder.comment("The rate at which steam is vented into the turbine.")
              .define("turbineVentGasFlow", 32_000D));
        turbineDisperserGasFlow = CachedDoubleValue.wrap(this, builder.comment("The rate at which steam is dispersed into the turbine.")
              .define("turbineDisperserGasFlow", 1_280D));
        condenserRate = CachedIntValue.wrap(this, builder.comment("The rate at which steam is condensed in the turbine.")
              .define("condenserRate", 64_000));
        builder.pop();

        builder.comment("Wind Generator Settings").push(WIND_CATEGORY);
        windGenerationMin = CachedFloatingLongValue.define(this, builder, "Minimum base generation value of the Wind Generator.",
              "windGenerationMin", FloatingLong.createConst(60));
        //TODO: Should this be capped by the min generator?
        windGenerationMax = CachedFloatingLongValue.define(this, builder, "Maximum base generation value of the Wind Generator.",
              "generationMax", FloatingLong.createConst(480));
        windGenerationMinY = CachedIntValue.wrap(this, builder.comment("The minimum Y value that affects the Wind Generators Power generation. This value gets clamped at the world's min height.")
              .defineInRange("minY", 24, DimensionType.MIN_Y, DimensionType.MAX_Y - 1));
        //Note: We just require that the maxY is greater than the minY, nothing goes badly if it is set above the max y of the world though
        // as it is just used for range clamping
        windGenerationMaxY = CachedIntValue.wrap(this, builder.comment("The maximum Y value that affects the Wind Generators Power generation. This value gets clamped at the world's logical height.")
              .define("maxY", DimensionType.MAX_Y, value -> value instanceof Integer && (Integer) value > windGenerationMinY.get()));
        //Note: We cannot verify the dimension exists as dimensions are dynamic so may not actually exist when we are validating
        windGenerationDimBlacklist = CachedResourceLocationListValue.define(this, builder.comment("The list of dimension ids that the Wind Generator will not generate power in."),
              "windGenerationDimBlacklist", rl -> true);
        builder.pop();

        builder.comment("Fusion Settings").push(FUSION_CATEGORY);
        fusionThermocoupleEfficiency = CachedDoubleValue.wrap(this, builder.comment("The fraction of the heat dissipated from the case that is converted to Joules.")
              .defineInRange("thermocoupleEfficiency", 0.05D, 0D, 1D));
        fusionCasingThermalConductivity = CachedDoubleValue.wrap(this, builder.comment("The fraction fraction of heat from the casing that can be transferred to all sources that are not water. Will impact max heat, heat transfer to thermodynamic conductors, and power generation.")
              .defineInRange("casingThermalConductivity", 0.1D, 0.001D, 1D));
        fusionWaterHeatingRatio = CachedDoubleValue.wrap(this, builder.comment("The fraction of the heat from the casing that is dissipated to water when water cooling is in use. Will impact max heat, and steam generation.")
              .defineInRange("waterHeatingRatio", 0.3D, 0D, 1D));
        builder.pop();

        builder.comment("Hohlraum Settings").push(HOHLRAUM_CATEGORY);
        hohlraumMaxGas = CachedLongValue.wrap(this, builder.comment("Hohlraum capacity in mB.")
              .defineInRange("maxGas", 10, 1, Long.MAX_VALUE));
        hohlraumFillRate = CachedLongValue.wrap(this, builder.comment("Amount of DT-Fuel Hohlraum can accept per tick.")
              .defineInRange("fillRate", 1, 1, Long.MAX_VALUE));
        builder.pop();

        builder.comment("Fission Reactor Settings").push(FISSION_CATEGORY);
        energyPerFissionFuel = CachedFloatingLongValue.define(this, builder, "Amount of energy created (in heat) from each whole mB of fission fuel.",
              "energyPerFissionFuel", FloatingLong.createConst(1_000_000));
        fissionCasingHeatCapacity = CachedDoubleValue.wrap(this, builder.comment("The heat capacity added to a Fission Reactor by a single casing block. Increase to require more energy to raise the reactor temperature.")
              .define("casingHeatCapacity", 1_000D));
        fissionSurfaceAreaTarget = CachedDoubleValue.wrap(this, builder.comment("The average surface area of a Fission Reactor's fuel assemblies to reach 100% boil efficiency. Higher values make it harder to cool the reactor.")
              .defineInRange("surfaceAreaTarget", 4D, 1D, Double.MAX_VALUE));
        fissionMeltdownsEnabled = CachedBooleanValue.wrap(this, builder.comment("Whether catastrophic meltdowns can occur from Fission Reactors. If disabled instead of melting down the reactor will turn off and not be able to be turned back on until the damage level decreases.")
              .define("meltdownsEnabled", true));
        fissionMeltdownChance = CachedDoubleValue.wrap(this, builder.comment("The chance of a meltdown occurring once damage passes 100%. Will linearly scale as damage continues increasing.")
              .defineInRange("meltdownChance", 0.001D, 0D, 1D));
        fissionMeltdownRadiationMultiplier = CachedDoubleValue.wrap(this, builder.comment("How much radioactivity of fuel/waste contents are multiplied during a meltdown.")
              .define("meltdownRadiationMultiplier", 50D));
        fissionPostMeltdownDamage = CachedDoubleValue.wrap(this, builder.comment("Damage to reset the reactor to after a meltdown.")
              .defineInRange("postMeltdownDamage", 0.75 * FissionReactorMultiblockData.MAX_DAMAGE, 0, FissionReactorMultiblockData.MAX_DAMAGE));
        defaultBurnRate = CachedDoubleValue.wrap(this, builder.comment("The default burn rate of the fission reactor.")
              .defineInRange("defaultBurnRate", 0.1D, 0.001D, 1D));
        burnPerAssembly = CachedLongValue.wrap(this, builder.comment("The burn rate increase each fuel assembly provides. Max Burn Rate = fuelAssemblies * burnPerAssembly")
              .defineInRange("burnPerAssembly", 1L, 1, 1_000_000));
        builder.pop();

        builder.pop();
        configSpec = builder.build();
    }

    @Override
    public String getFileName() {
        return "generators";
    }

    @Override
    public ForgeConfigSpec getConfigSpec() {
        return configSpec;
    }

    @Override
    public Type getConfigType() {
        return Type.SERVER;
    }
}
