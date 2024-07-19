package mekanism.client.render.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekanism.client.render.RenderResizableCuboid.FaceDisplay;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import mekanism.common.util.EnumUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.profiling.ProfilerFiller;

@ParametersAreNonnullByDefault
public class RenderDigitalMiner extends MekanismTileEntityRenderer<TileEntityDigitalMiner> {

    private static Model3D model;
    private static final int[] colors = new int[EnumUtils.DIRECTIONS.length];

    static {
        colors[Direction.DOWN.ordinal()] = MekanismRenderer.getColorARGB(255, 255, 255, 0.82F);
        colors[Direction.UP.ordinal()] = MekanismRenderer.getColorARGB(255, 255, 255, 0.82F);
        colors[Direction.NORTH.ordinal()] = MekanismRenderer.getColorARGB(255, 255, 255, 0.8F);
        colors[Direction.SOUTH.ordinal()] = MekanismRenderer.getColorARGB(255, 255, 255, 0.8F);
        colors[Direction.WEST.ordinal()] = MekanismRenderer.getColorARGB(255, 255, 255, 0.78F);
        colors[Direction.EAST.ordinal()] = MekanismRenderer.getColorARGB(255, 255, 255, 0.78F);
    }

    public static void resetCachedVisuals() {
        model = null;
    }

    public RenderDigitalMiner(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void render(TileEntityDigitalMiner miner, float partialTick, PoseStack matrix, MultiBufferSource renderer, int light, int overlayLight, ProfilerFiller profiler) {
        if (miner.isClientRendering() && miner.canDisplayVisuals()) {
            if (model == null) {
                model = new Model3D();
                model.setTexture(MekanismRenderer.whiteIcon);
                model.minX = 0;
                model.minY = 0;
                model.minZ = 0;
                model.maxX = 1;
                model.maxY = 1;
                model.maxZ = 1;
            }
            matrix.pushPose();
            //Adjust translation and scale ever so slightly so that no z-fighting happens at the edges if there are blocks there
            matrix.translate(-miner.getRadius() + 0.01, miner.getMinY() - miner.getBlockPos().getY() + 0.01, -miner.getRadius() + 0.01);
            float diameter = miner.getDiameter() - 0.02F;
            matrix.scale(diameter, miner.getMaxY() - miner.getMinY() - 0.02F, diameter);
            //If we are inside the visualization we don't have to render the "front" face, otherwise we need to render both given how the visualization works
            // we want to be able to see all faces easily
            FaceDisplay faceDisplay = isInsideBounds(miner.getBlockPos().getX() - miner.getRadius(), miner.getMinY(), miner.getBlockPos().getZ() - miner.getRadius(),
                  miner.getBlockPos().getX() + miner.getRadius() + 1, miner.getMaxY(), miner.getBlockPos().getZ() + miner.getRadius() + 1)
                                      ? FaceDisplay.BACK : FaceDisplay.BOTH;
            MekanismRenderer.renderObject(model, matrix, renderer.getBuffer(Sheets.translucentCullBlockSheet()), colors, MekanismRenderer.FULL_LIGHT, overlayLight,
                  faceDisplay);
            matrix.popPose();
        }
    }

    @Override
    protected String getProfilerSection() {
        return ProfilerConstants.DIGITAL_MINER;
    }

    @Override
    public boolean shouldRenderOffScreen(TileEntityDigitalMiner tile) {
        return true;
    }
}