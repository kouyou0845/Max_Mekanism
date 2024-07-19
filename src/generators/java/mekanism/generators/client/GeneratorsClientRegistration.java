package mekanism.generators.client;

import mekanism.api.MekanismAPI;
import mekanism.api.gear.IModuleHelper;
import mekanism.client.ClientRegistration;
import mekanism.client.ClientRegistrationUtil;
import mekanism.client.model.baked.ExtensionBakedModel.TransformedBakedModel;
import mekanism.client.render.lib.QuadTransformation;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.FluidRegistryObject;
import mekanism.generators.client.gui.GuiBioGenerator;
import mekanism.generators.client.gui.GuiFissionReactor;
import mekanism.generators.client.gui.GuiFissionReactorLogicAdapter;
import mekanism.generators.client.gui.GuiFissionReactorStats;
import mekanism.generators.client.gui.GuiFusionReactorController;
import mekanism.generators.client.gui.GuiFusionReactorFuel;
import mekanism.generators.client.gui.GuiFusionReactorHeat;
import mekanism.generators.client.gui.GuiFusionReactorLogicAdapter;
import mekanism.generators.client.gui.GuiFusionReactorStats;
import mekanism.generators.client.gui.GuiGasGenerator;
import mekanism.generators.client.gui.GuiHeatGenerator;
import mekanism.generators.client.gui.GuiIndustrialTurbine;
import mekanism.generators.client.gui.GuiSolarGenerator;
import mekanism.generators.client.gui.GuiTurbineStats;
import mekanism.generators.client.gui.GuiWindGenerator;
import mekanism.generators.client.model.ModelTurbine;
import mekanism.generators.client.model.ModelWindGenerator;
import mekanism.generators.client.render.RenderBioGenerator;
import mekanism.generators.client.render.RenderFissionReactor;
import mekanism.generators.client.render.RenderFusionReactor;
import mekanism.generators.client.render.RenderIndustrialTurbine;
import mekanism.generators.client.render.RenderTurbineRotor;
import mekanism.generators.client.render.RenderWindGenerator;
import mekanism.generators.client.render.item.RenderWindGeneratorItem;
import mekanism.generators.common.MekanismGenerators;
import mekanism.generators.common.registries.GeneratorsBlocks;
import mekanism.generators.common.registries.GeneratorsContainerTypes;
import mekanism.generators.common.registries.GeneratorsFluids;
import mekanism.generators.common.registries.GeneratorsModules;
import mekanism.generators.common.registries.GeneratorsTileEntityTypes;
import mekanism.generators.common.tile.TileEntityAdvancedSolarGenerator;
import mekanism.generators.common.tile.TileEntitySolarGenerator;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = MekanismGenerators.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GeneratorsClientRegistration {

    private GeneratorsClientRegistration() {
    }

    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        //Block render layers
        ClientRegistrationUtil.setRenderLayer(RenderType.translucent(), GeneratorsBlocks.LASER_FOCUS_MATRIX, GeneratorsBlocks.REACTOR_GLASS);
        ClientRegistrationUtil.setRenderLayer(renderType -> renderType == RenderType.solid() || renderType == RenderType.translucent(),
              GeneratorsBlocks.BIO_GENERATOR, GeneratorsBlocks.HEAT_GENERATOR);
        //Fluids (translucent)
        for (FluidRegistryObject<?, ?, ?, ?> fluidRO : GeneratorsFluids.FLUIDS.getAllFluids()) {
            ClientRegistrationUtil.setRenderLayer(RenderType.translucent(), fluidRO);
        }

        // adv solar gen requires to be translated up 1 block, so handle the model separately
        ClientRegistration.addCustomModel(GeneratorsBlocks.ADVANCED_SOLAR_GENERATOR, (orig, evt) -> new TransformedBakedModel<Void>(orig,
              QuadTransformation.translate(new Vec3(0, 1, 0))));

        IModuleHelper moduleHelper = MekanismAPI.getModuleHelper();
        moduleHelper.addMekaSuitModuleModels(MekanismGenerators.rl("models/entity/mekasuit_modules.obj"));
        moduleHelper.addMekaSuitModuleModelSpec("solar_helmet", GeneratorsModules.SOLAR_RECHARGING_UNIT, EquipmentSlot.HEAD);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GeneratorsTileEntityTypes.BIO_GENERATOR.get(), RenderBioGenerator::new);
        event.registerBlockEntityRenderer(GeneratorsTileEntityTypes.FUSION_REACTOR_CONTROLLER.get(), RenderFusionReactor::new);
        event.registerBlockEntityRenderer(GeneratorsTileEntityTypes.FISSION_REACTOR_CASING.get(), RenderFissionReactor::new);
        event.registerBlockEntityRenderer(GeneratorsTileEntityTypes.FISSION_REACTOR_PORT.get(), RenderFissionReactor::new);
        event.registerBlockEntityRenderer(GeneratorsTileEntityTypes.FISSION_REACTOR_LOGIC_ADAPTER.get(), RenderFissionReactor::new);
        event.registerBlockEntityRenderer(GeneratorsTileEntityTypes.TURBINE_CASING.get(), RenderIndustrialTurbine::new);
        event.registerBlockEntityRenderer(GeneratorsTileEntityTypes.TURBINE_ROTOR.get(), RenderTurbineRotor::new);
        event.registerBlockEntityRenderer(GeneratorsTileEntityTypes.TURBINE_VALVE.get(), RenderIndustrialTurbine::new);
        event.registerBlockEntityRenderer(GeneratorsTileEntityTypes.TURBINE_VENT.get(), RenderIndustrialTurbine::new);
        event.registerBlockEntityRenderer(GeneratorsTileEntityTypes.WIND_GENERATOR.get(), RenderWindGenerator::new);
    }

    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModelWindGenerator.GENERATOR_LAYER, ModelWindGenerator::createLayerDefinition);
        event.registerLayerDefinition(ModelTurbine.TURBINE_LAYER, ModelTurbine::createLayerDefinition);
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(RenderWindGeneratorItem.RENDERER);
    }

    @SuppressWarnings("Convert2MethodRef")
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerContainers(RegistryEvent.Register<MenuType<?>> event) {
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.BIO_GENERATOR, GuiBioGenerator::new);
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.GAS_BURNING_GENERATOR, GuiGasGenerator::new);
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.HEAT_GENERATOR, GuiHeatGenerator::new);
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.INDUSTRIAL_TURBINE, GuiIndustrialTurbine::new);
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.FISSION_REACTOR, GuiFissionReactor::new);
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.FISSION_REACTOR_STATS, GuiFissionReactorStats::new);
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.FISSION_REACTOR_LOGIC_ADAPTER, GuiFissionReactorLogicAdapter::new);
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.FUSION_REACTOR_CONTROLLER, GuiFusionReactorController::new);
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.FUSION_REACTOR_FUEL, GuiFusionReactorFuel::new);
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.FUSION_REACTOR_HEAT, GuiFusionReactorHeat::new);
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.FUSION_REACTOR_LOGIC_ADAPTER, GuiFusionReactorLogicAdapter::new);
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.FUSION_REACTOR_STATS, GuiFusionReactorStats::new);
        // for some reason java is unable to infer the types with this generics structure, so we include constructor signature ourselves
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.SOLAR_GENERATOR, (MekanismTileContainer<TileEntitySolarGenerator> container, Inventory inv, Component title) -> new GuiSolarGenerator<>(container, inv, title));
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.ADVANCED_SOLAR_GENERATOR, (MekanismTileContainer<TileEntityAdvancedSolarGenerator> container, Inventory inv, Component title) -> new GuiSolarGenerator<>(container, inv, title));
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.TURBINE_STATS, GuiTurbineStats::new);
        ClientRegistrationUtil.registerScreen(GeneratorsContainerTypes.WIND_GENERATOR, GuiWindGenerator::new);
    }

    @SubscribeEvent
    public static void onStitch(TextureStitchEvent.Pre event) {
        if (!event.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS)) {
            return;
        }
        RenderBioGenerator.resetCachedModels();
        RenderFissionReactor.resetCachedModels();
        GeneratorsSpecialColors.GUI_OBJECTS.parse(MekanismGenerators.rl("textures/colormap/gui_objects.png"));
    }
}