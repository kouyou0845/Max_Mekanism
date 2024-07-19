package mekanism.client.render.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekanism.client.render.RenderResizableCuboid.FaceDisplay;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.tile.machine.TileEntityDimensionalStabilizer;
import mekanism.common.util.EnumUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@ParametersAreNonnullByDefault
public class RenderDimensionalStabilizer extends MekanismTileEntityRenderer<TileEntityDimensionalStabilizer> {

    private static Model3D model;
    private static final int[] colors = new int[EnumUtils.DIRECTIONS.length];

    static {
        //Note: We skip up and down as we never render them so no need to set the color
        colors[Direction.NORTH.ordinal()] = MekanismRenderer.getColorARGB(255, 255, 255, 0.82F);
        colors[Direction.SOUTH.ordinal()] = MekanismRenderer.getColorARGB(255, 255, 255, 0.82F);
        colors[Direction.WEST.ordinal()] = MekanismRenderer.getColorARGB(255, 255, 255, 0.78F);
        colors[Direction.EAST.ordinal()] = MekanismRenderer.getColorARGB(255, 255, 255, 0.78F);
        //TODO: At some point experiment with different colors to try and improve rendering of it when in a checkerboard pattern
        // so that it is clearer which ones are rendering and which are not, or maybe evaluate actually having the top and bottom render
    }

    public static void resetCachedVisuals() {
        model = null;
    }

    public RenderDimensionalStabilizer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void render(TileEntityDimensionalStabilizer stabilizer, float partialTick, PoseStack matrix, MultiBufferSource renderer, int light, int overlayLight,
          ProfilerFiller profiler) {
        if (model == null) {
            model = new Model3D();
            model.setTexture(MekanismRenderer.whiteIcon);
            //Don't bother rendering the top or the bottom as it is always at world bounds
            model.setSideRender(Direction.UP, false);
            model.setSideRender(Direction.DOWN, false);
            model.minX = 0;
            model.minY = 0;
            model.minZ = 0;
            model.maxX = 1;
            model.maxY = 1;
            model.maxZ = 1;
        }
        //Calculate the different sides that should be rendered, as a 3D array. The last parameter is of length 5 to support the four cardinal directions
        // PLUS a marker for if the chunk is loaded and should be rendered at all. As if a chunk is surrounded on all sides by other chunks, then none of
        // its sides will actually need to be drawn, but it still should be able to combine with neighboring pieces
        boolean[][][] allRenderSides = new boolean[TileEntityDimensionalStabilizer.MAX_LOAD_DIAMETER][TileEntityDimensionalStabilizer.MAX_LOAD_DIAMETER][5];
        for (int x = 0; x < allRenderSides.length; x++) {
            boolean[][] rowRenderSides = allRenderSides[x];
            for (int z = 0; z < rowRenderSides.length; z++) {
                if (stabilizer.isChunkLoadingAt(x, z)) {
                    boolean[] renderSides = rowRenderSides[z];
                    Arrays.fill(renderSides, true);
                    //Look back at the previous row and previous column if they are touching then the side should be disabled
                    // for rendering for both this one and the previous
                    if (x > 0) {
                        boolean[] previousRenderSides = allRenderSides[x - 1][z];
                        if (previousRenderSides[Direction.EAST.get2DDataValue()]) {
                            renderSides[Direction.WEST.get2DDataValue()] = false;
                            previousRenderSides[Direction.EAST.get2DDataValue()] = false;
                        }
                    }
                    if (z > 0) {
                        boolean[] previousRenderSides = rowRenderSides[z - 1];
                        if (previousRenderSides[Direction.SOUTH.get2DDataValue()]) {
                            renderSides[Direction.NORTH.get2DDataValue()] = false;
                            previousRenderSides[Direction.SOUTH.get2DDataValue()] = false;
                        }
                    }
                }
            }
        }
        Level level = stabilizer.getLevel();
        int minY = level.getMinBuildHeight();
        int height = level.getMaxBuildHeight() - minY;
        BlockPos pos = stabilizer.getBlockPos();
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        VertexConsumer buffer = renderer.getBuffer(Sheets.translucentCullBlockSheet());
        for (RenderPiece piece : calculateRenderPieces(allRenderSides)) {
            //Set the visibility of the sides that are going to render for this piece
            model.setSideRender(Direction.NORTH, piece.renderNorth);
            model.setSideRender(Direction.EAST, piece.renderEast);
            model.setSideRender(Direction.SOUTH, piece.renderSouth);
            model.setSideRender(Direction.WEST, piece.renderWest);
            int xChunkOffset = piece.x - TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS;
            int zChunkOffset = piece.z - TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS;
            ChunkPos startChunk = new ChunkPos(chunkX + xChunkOffset, chunkZ + zChunkOffset);
            ChunkPos endChunk = new ChunkPos(startChunk.x + piece.xLength - 1, startChunk.z + piece.zLength - 1);
            //Adjust translation and scale ever so slightly so that no z-fighting happens at the edges if there are blocks there
            double xShift = 0.01, zShift = 0.01;
            float xScaleShift = 0.02F, zScaleShift = 0.02F;
            if (piece.renderEast && piece.renderWest && !piece.renderNorth) {
                //North and maybe South walls missing
                zShift = -0.01;
                zScaleShift = piece.renderSouth ? 0 : -0.02F;
            } else if (piece.renderNorth && !piece.renderSouth) {
                //South and maybe East and/or West walls missing
                zScaleShift = 0;
            } else if (piece.renderNorth && piece.renderWest && !piece.renderEast) {
                //East wall missing
                xScaleShift = 0;
            } else if (piece.renderNorth && !piece.renderWest) {
                //West and maybe East walls missing
                xShift = -0.01;
                xScaleShift = piece.renderEast ? 0 : -0.02F;
            } else if (piece.renderSouth && piece.renderEast != piece.renderWest) {
                //North and either East or West walls missing
                zShift = -0.01;
                zScaleShift = 0;
            }
            matrix.pushPose();
            matrix.translate(startChunk.getMinBlockX() - pos.getX() + xShift, minY - pos.getY(), startChunk.getMinBlockZ() - pos.getZ() + zShift);
            matrix.scale(16 * piece.xLength - xScaleShift, height, 16 * piece.zLength - zScaleShift);
            //If we are inside the visualization we don't have to render the "front" face, otherwise we need to render both given how the visualization works
            // we want to be able to see all faces easily
            FaceDisplay faceDisplay = isInsideBounds(startChunk.getMinBlockX(), Double.NEGATIVE_INFINITY, startChunk.getMinBlockZ(),
                  endChunk.getMaxBlockX() + 1, Double.POSITIVE_INFINITY, endChunk.getMaxBlockZ() + 1) ? FaceDisplay.BACK : FaceDisplay.BOTH;
            MekanismRenderer.renderObject(model, matrix, buffer, colors, MekanismRenderer.FULL_LIGHT, overlayLight, faceDisplay);
            matrix.popPose();
        }
    }

    @Override
    protected String getProfilerSection() {
        return ProfilerConstants.DIMENSIONAL_STABILIZER;
    }

    @Override
    public boolean shouldRenderOffScreen(TileEntityDimensionalStabilizer tile) {
        return true;
    }

    @Override
    public boolean shouldRender(TileEntityDimensionalStabilizer tile, Vec3 camera) {
        return tile.isClientRendering() && tile.canDisplayVisuals() && super.shouldRender(tile, camera);
    }

    /**
     * Combines the sides that should be rendered for each chunk's position into a list of pieces that should be rendered. This allows for doing less draw calls and
     * overall better performance.
     *
     * @param allRenderSides 3D array of ROW, COLUMN, RENDER
     *
     * @return List of render pieces.
     */
    private List<RenderPiece> calculateRenderPieces(boolean[][][] allRenderSides) {
        //Keep track of the minimal amount of data that is needed to match in order to merge two column pieces across rows
        record MinimalColumnPieceData(int z, int zLength, boolean renderNorth, boolean renderSouth) {
        }
        //Used to store the remaining data for what rows different column pieces were found and a bit of side render data
        record MinimalRowPieceData(int x, boolean renderEast, boolean renderWest) {
        }
        Map<MinimalColumnPieceData, List<MinimalRowPieceData>> columnData = new HashMap<>();
        for (int x = 0; x < allRenderSides.length; x++) {
            boolean[][] rowRenderSides = allRenderSides[x];
            for (int z = 0; z < rowRenderSides.length; ) {
                int zLength = 1;
                boolean[] renderSides = rowRenderSides[z];
                //Only calculate if the chunk is loaded, and we are meant to add it as part of the pieces to render
                if (renderSides[4]) {
                    boolean renderNorth = renderSides[Direction.NORTH.get2DDataValue()];
                    boolean renderSouth = renderSides[Direction.SOUTH.get2DDataValue()];
                    boolean renderEast = renderSides[Direction.EAST.get2DDataValue()];
                    boolean renderWest = renderSides[Direction.WEST.get2DDataValue()];
                    //If we don't need to render a side which means we potentially may be able to merge
                    while (!renderSouth && z + zLength < rowRenderSides.length) {
                        boolean[] nextColumnRenderSides = rowRenderSides[z + zLength];
                        //Note: We don't need to check if the back side is connected as we know from how we built it that it won't be
                        if (renderEast == nextColumnRenderSides[Direction.EAST.get2DDataValue()] &&
                            renderWest == nextColumnRenderSides[Direction.WEST.get2DDataValue()]) {
                            zLength++;
                            //Update if we render on the south side as if we are we can exit because the next column piece
                            // should not be present
                            renderSouth = nextColumnRenderSides[Direction.SOUTH.get2DDataValue()];
                        } else {
                            break;
                        }
                    }
                    columnData.computeIfAbsent(new MinimalColumnPieceData(z, zLength, renderNorth, renderSouth),
                                piece -> new ArrayList<>(TileEntityDimensionalStabilizer.MAX_LOAD_DIAMETER))
                          .add(new MinimalRowPieceData(x, renderEast, renderWest));

                }
                z += zLength;
            }
        }
        //Combine pieces of column data
        List<RenderPiece> pieces = new ArrayList<>();
        for (Map.Entry<MinimalColumnPieceData, List<MinimalRowPieceData>> entry : columnData.entrySet()) {
            MinimalColumnPieceData minimalColumnPiece = entry.getKey();
            List<MinimalRowPieceData> rows = entry.getValue();
            for (int row = 0; row < rows.size(); ) {
                int xLength = 1;
                MinimalRowPieceData minimalRowPiece = rows.get(row);
                boolean renderEast = minimalRowPiece.renderEast;
                while (!renderEast && row + xLength < rows.size()) {
                    MinimalRowPieceData nextRowPiece = rows.get(row + xLength);
                    //Note: comparing north and south pieces happens at z and zLength comparison of the map's keys,
                    // so we only have to confirm that the two column pieces are in neighboring rows
                    if (minimalRowPiece.x + xLength == nextRowPiece.x) {
                        xLength++;
                        //Update if we render on the east side as if we are we can exit because the next
                        // column row data should not be present
                        renderEast = nextRowPiece.renderEast;
                    } else {
                        break;
                    }
                }
                pieces.add(new RenderPiece(minimalRowPiece.x, xLength, minimalColumnPiece.z, minimalColumnPiece.zLength, minimalColumnPiece.renderNorth,
                      minimalColumnPiece.renderSouth, renderEast, minimalRowPiece.renderWest));
                row += xLength;
            }
        }
        return pieces;
    }

    private record RenderPiece(int x, int xLength, int z, int zLength, boolean renderNorth, boolean renderSouth, boolean renderEast, boolean renderWest) {
    }
}