package mekanism.generators.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekanism.client.render.ModelRenderer;
import mekanism.client.render.RenderResizableCuboid.FaceDisplay;
import mekanism.client.render.data.ChemicalRenderData.GasRenderData;
import mekanism.client.render.data.FluidRenderData;
import mekanism.client.render.data.RenderData;
import mekanism.client.render.tileentity.MekanismTileEntityRenderer;
import mekanism.generators.common.GeneratorsProfilerConstants;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import mekanism.generators.common.content.fission.FissionReactorValidator.FormedAssembly;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorCasing;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;

@ParametersAreNonnullByDefault
public class RenderFissionReactor extends MekanismTileEntityRenderer<TileEntityFissionReactorCasing> {

    private static final Map<RenderData, Model3D> cachedHeatedCoolantModels = new Object2ObjectOpenHashMap<>();
    private static final int GLOW_ARGB = MekanismRenderer.getColorARGB(0.466F, 0.882F, 0.929F, 0.6F);
    private static Model3D glowModel;

    public static void resetCachedModels() {
        cachedHeatedCoolantModels.clear();
        glowModel = null;
    }

    public RenderFissionReactor(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void render(TileEntityFissionReactorCasing tile, float partialTick, PoseStack matrix, MultiBufferSource renderer, int light, int overlayLight,
          ProfilerFiller profiler) {
        if (tile.isMaster()) {
            FissionReactorMultiblockData multiblock = tile.getMultiblock();
            if (multiblock.isFormed() && multiblock.renderLocation != null) {
                BlockPos pos = tile.getBlockPos();
                VertexConsumer buffer = renderer.getBuffer(Sheets.translucentCullBlockSheet());
                if (multiblock.isBurning()) {
                    //TODO: Convert the glow model and stuff to being part of the baked model and using model data
                    // as I am fairly sure that should give a decent boost to performance
                    if (glowModel == null) {
                        glowModel = new Model3D();
                        glowModel.minX = 0.05F;
                        glowModel.minY = 0.01F;
                        glowModel.minZ = 0.05F;
                        glowModel.maxX = 0.95F;
                        glowModel.maxY = 0.99F;
                        glowModel.maxZ = 0.95F;
                        glowModel.setTexture(MekanismRenderer.whiteIcon);
                    }
                    for (FormedAssembly assembly : multiblock.assemblies) {
                        matrix.pushPose();
                        matrix.translate(assembly.pos().getX() - pos.getX(), assembly.pos().getY() - pos.getY(), assembly.pos().getZ() - pos.getZ());
                        matrix.scale(1, assembly.height(), 1);
                        MekanismRenderer.renderObject(glowModel, matrix, buffer, GLOW_ARGB, MekanismRenderer.FULL_LIGHT, overlayLight, FaceDisplay.FRONT);
                        matrix.popPose();
                    }
                }
                if (!multiblock.fluidCoolantTank.isEmpty()) {
                    int height = multiblock.height() - 2;
                    if (height >= 1) {
                        FluidRenderData data = new FluidRenderData(multiblock.fluidCoolantTank.getFluid());
                        int glow = setCoolantDataAndCalculateGlow(data, multiblock, height);
                        matrix.pushPose();
                        matrix.translate(data.location.getX() - pos.getX(), data.location.getY() - pos.getY(), data.location.getZ() - pos.getZ());
                        Model3D model = ModelRenderer.getModel(data, multiblock.prevCoolantScale);
                        MekanismRenderer.renderObject(model, matrix, buffer, data.getColorARGB(multiblock.prevCoolantScale), glow, overlayLight, getFaceDisplay(data, model));
                        matrix.popPose();
                        MekanismRenderer.renderValves(matrix, buffer, multiblock.valves, data, pos, glow, overlayLight, isInsideMultiblock(data));
                    }
                }
                if (!multiblock.heatedCoolantTank.isEmpty()) {
                    int height = multiblock.height() - 2;
                    if (height >= 1) {
                        GasRenderData data = new GasRenderData(multiblock.heatedCoolantTank.getStack());
                        int glow = setCoolantDataAndCalculateGlow(data, multiblock, height);
                        Model3D gasModel;
                        if (cachedHeatedCoolantModels.containsKey(data)) {
                            gasModel = cachedHeatedCoolantModels.get(data);
                        } else {
                            //Create a slightly shrunken version of the model if it is missing to prevent z-fighting
                            gasModel = ModelRenderer.getModel(data, 1).copy();
                            gasModel.minX += 0.01F;
                            gasModel.minY += 0.01F;
                            gasModel.minZ += 0.01F;
                            gasModel.maxX -= 0.01F;
                            gasModel.maxY -= 0.01F;
                            gasModel.maxZ -= 0.01F;
                            cachedHeatedCoolantModels.put(data, gasModel);
                        }
                        matrix.pushPose();
                        matrix.translate(data.location.getX() - pos.getX(), data.location.getY() - pos.getY(), data.location.getZ() - pos.getZ());
                        MekanismRenderer.renderObject(gasModel, matrix, buffer, data.getColorARGB(multiblock.prevHeatedCoolantScale), glow, overlayLight,
                              getFaceDisplay(data, gasModel));
                        matrix.popPose();
                    }
                }
            }
        }
    }

    private int setCoolantDataAndCalculateGlow(RenderData data, FissionReactorMultiblockData multiblock, int height) {
        data.location = multiblock.renderLocation;
        data.height = height;
        data.length = multiblock.length();
        data.width = multiblock.width();
        return data.calculateGlowLight(MekanismRenderer.FULL_SKY_LIGHT);
    }

    @Override
    protected String getProfilerSection() {
        return GeneratorsProfilerConstants.FISSION_REACTOR;
    }

    @Override
    public boolean shouldRenderOffScreen(TileEntityFissionReactorCasing tile) {
        if (tile.isMaster()) {
            FissionReactorMultiblockData multiblock = tile.getMultiblock();
            return multiblock.isFormed() && multiblock.renderLocation != null;
        }
        return false;
    }
}