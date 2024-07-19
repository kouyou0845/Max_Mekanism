package mekanism.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nonnull;
import mekanism.api.MekanismAPI;
import mekanism.api.RelativeSide;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekanism.client.render.RenderResizableCuboid.FaceDisplay;
import mekanism.client.render.armor.ISpecialGear;
import mekanism.client.render.armor.MekaSuitArmor;
import mekanism.client.render.lib.Quad;
import mekanism.client.render.lib.QuadUtils;
import mekanism.client.render.lib.Vertex;
import mekanism.client.render.lib.effect.BoltRenderer;
import mekanism.client.render.tileentity.IWireFrameRenderer;
import mekanism.common.Mekanism;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.block.BlockBounding;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeCustomSelectionBox;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.content.gear.IBlastingItem;
import mekanism.common.content.gear.IModuleContainerItem;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.item.ItemConfigurator.ConfiguratorMode;
import mekanism.common.item.gear.ItemFlamethrower;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import mekanism.common.item.interfaces.IModeItem;
import mekanism.common.lib.Color;
import mekanism.common.lib.effect.BoltEffect;
import mekanism.common.lib.math.Pos3D;
import mekanism.common.lib.radiation.RadiationManager;
import mekanism.common.lib.radiation.RadiationManager.RadiationScale;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismParticleTypes;
import mekanism.common.tile.TileEntityBoundingBlock;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.interfaces.ISideConfiguration;
import mekanism.common.util.ChemicalUtil;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.WorldUtils;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.RenderProperties;
import net.minecraftforge.client.event.DrawSelectionEvent;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RenderTickHandler {

    public final Minecraft minecraft = Minecraft.getInstance();

    private static final Map<BlockState, List<Vertex[]>> cachedWireFrames = new HashMap<>();
    private static final Map<Direction, Map<TransmissionType, Model3D>> cachedOverlays = new EnumMap<>(Direction.class);

    public static int modeSwitchTimer = 0;
    public static double prevRadiation = 0;

    private static final BoltRenderer boltRenderer = new BoltRenderer();

    private boolean outliningArea = false;

    public static void resetCached() {
        cachedOverlays.clear();
        cachedWireFrames.clear();
    }

    public static void renderBolt(Object renderer, BoltEffect bolt) {
        boltRenderer.update(renderer, bolt, MekanismRenderer.getPartialTick());
    }

    //Note: This listener is only registered if JEI is loaded
    public static void guiOpening(ScreenOpenEvent event) {
        if (Minecraft.getInstance().screen instanceof GuiMekanism screen) {
            //If JEI is loaded and our current screen is a mekanism gui,
            // check if the new screen is a JEI recipe screen
            if (event.getScreen() instanceof IRecipesGui) {
                //If it is mark on our current screen that we are switching to JEI
                screen.switchingToJEI = true;
            }
        }
    }

    @SubscribeEvent
    public void filterTooltips(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof IModuleContainerItem item) {
            item.filterTooltips(stack, event.getToolTip());
        }
    }

    @SubscribeEvent
    public void renderWorld(RenderLevelLastEvent event) {
        if (boltRenderer.hasBoltsToRender()) {
            //Only do matrix transforms and mess with buffers if we actually have any bolts to render
            PoseStack matrix = event.getPoseStack();
            matrix.pushPose();
            // here we translate based on the inverse position of the client viewing camera to get back to 0, 0, 0
            Vec3 camVec = minecraft.gameRenderer.getMainCamera().getPosition();
            matrix.translate(-camVec.x, -camVec.y, -camVec.z);
            //TODO: FIXME, this doesn't work on fabulous, I think it needs something like
            // https://github.com/MinecraftForge/MinecraftForge/pull/7225
            MultiBufferSource.BufferSource renderer = minecraft.renderBuffers().bufferSource();
            boltRenderer.render(event.getPartialTick(), matrix, renderer);
            renderer.endBatch(MekanismRenderType.MEK_LIGHTNING);
            matrix.popPose();
        }
    }

    @SubscribeEvent
    public void renderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.getItem() instanceof ItemMekaSuitArmor armorItem) {
            MekaSuitArmor armor = (MekaSuitArmor) ((ISpecialGear) RenderProperties.get(armorItem)).getGearModel(EquipmentSlot.CHEST);
            PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            PlayerModel<AbstractClientPlayer> model = renderer.getModel();
            model.setAllVisible(true);
            //Note: We just want it to act as empty even if there is a map as it looks a lot better
            boolean rightHand = event.getArm() == HumanoidArm.RIGHT;
            if (rightHand) {
                model.rightArmPose = ArmPose.EMPTY;
            } else {
                model.leftArmPose = ArmPose.EMPTY;
            }
            model.attackTime = 0.0F;
            model.crouching = false;
            model.swimAmount = 0.0F;
            model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
            armor.renderArm(model, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY, chestStack.hasFoil(), player, rightHand);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void tickEnd(RenderTickEvent event) {
        if (event.phase == Phase.END) {
            if (minecraft.player != null && minecraft.player.level != null && !minecraft.isPaused()) {
                Player player = minecraft.player;
                Level world = minecraft.player.level;
                //TODO: Check if we have another matrix stack we should use
                PoseStack matrix = new PoseStack();
                renderStatusBar(matrix, player);
                //Traverse active jetpacks and do animations
                for (UUID uuid : Mekanism.playerState.getActiveJetpacks()) {
                    Player p = world.getPlayerByUUID(uuid);
                    if (p != null) {
                        Pos3D playerPos = new Pos3D(p).translate(0, p.getEyeHeight(), 0);
                        Vec3 playerMotion = p.getDeltaMovement();
                        float random = (world.random.nextFloat() - 0.5F) * 0.1F;
                        //This positioning code is somewhat cursed but it seems to be mostly working and entity pose code seems cursed in general
                        float xRot;
                        if (p.isCrouching()) {
                            xRot = 20;
                            playerPos = playerPos.translate(0, 0.125, 0);
                        } else {
                            float f = p.getSwimAmount(event.renderTickTime);
                            if (p.isFallFlying()) {
                                float f1 = (float) p.getFallFlyingTicks() + event.renderTickTime;
                                float f2 = Mth.clamp(f1 * f1 / 100.0F, 0.0F, 1.0F);
                                xRot = f2 * (-90.0F - p.getXRot());
                            } else {
                                float f3 = p.isInWater() ? -90.0F - p.getXRot() : -90.0F;
                                xRot = Mth.lerp(f, 0.0F, f3);
                            }
                            xRot = -xRot;
                            Pos3D eyeAdjustments;
                            if (p.isFallFlying() && (p != player || !minecraft.options.getCameraType().isFirstPerson())) {
                                eyeAdjustments = new Pos3D(0, p.getEyeHeight(Pose.STANDING), 0).xRot(xRot).yRot(p.yBodyRot);
                            } else if (p.isVisuallySwimming()) {
                                eyeAdjustments = new Pos3D(0, p.getEyeHeight(), 0).xRot(xRot).yRot(p.yBodyRot).translate(0, 0.5, 0);
                            } else {
                                eyeAdjustments = new Pos3D(0, p.getEyeHeight(), 0).xRot(xRot).yRot(p.yBodyRot);
                            }
                            playerPos = new Pos3D(p.getX() + eyeAdjustments.x, p.getY() + eyeAdjustments.y, p.getZ() + eyeAdjustments.z);
                        }
                        Pos3D vLeft = new Pos3D(-0.43, -0.55, -0.54).xRot(xRot).yRot(p.yBodyRot);
                        renderJetpackSmoke(world, playerPos.translate(vLeft, playerMotion), vLeft.scale(0.2).translate(playerMotion, vLeft.scale(random)));
                        Pos3D vRight = new Pos3D(0.43, -0.55, -0.54).xRot(xRot).yRot(p.yBodyRot);
                        renderJetpackSmoke(world, playerPos.translate(vRight, playerMotion), vRight.scale(0.2).translate(playerMotion, vRight.scale(random)));
                        Pos3D vCenter = new Pos3D((world.random.nextFloat() - 0.5) * 0.4, -0.86, -0.30).xRot(xRot).yRot(p.yBodyRot);
                        renderJetpackSmoke(world, playerPos.translate(vCenter, playerMotion), vCenter.scale(0.2).translate(playerMotion));
                    }
                }

                if (world.getGameTime() % 4 == 0) {
                    //Traverse active scuba masks and do animations
                    for (UUID uuid : Mekanism.playerState.getActiveScubaMasks()) {
                        Player p = world.getPlayerByUUID(uuid);
                        if (p != null && p.isInWater()) {
                            Pos3D vec = new Pos3D(0.4, 0.4, 0.4).multiply(p.getViewVector(1)).translate(0, -0.2, 0);
                            Pos3D motion = vec.scale(0.2).translate(p.getDeltaMovement());
                            Pos3D v = new Pos3D(p).translate(0, p.getEyeHeight(), 0).translate(vec);
                            world.addParticle(MekanismParticleTypes.SCUBA_BUBBLE.get(), v.x, v.y, v.z, motion.x, motion.y + 0.2, motion.z);
                        }
                    }
                    //Traverse players and do animations for idle flame throwers
                    for (Player p : world.players()) {
                        if (!p.swinging && !Mekanism.playerState.isFlamethrowerOn(p)) {
                            ItemStack currentItem = p.getMainHandItem();
                            if (!currentItem.isEmpty() && currentItem.getItem() instanceof ItemFlamethrower && ChemicalUtil.hasGas(currentItem)) {
                                Pos3D flameVec;
                                boolean rightHanded = p.getMainArm() == HumanoidArm.RIGHT;
                                if (player == p && minecraft.options.getCameraType().isFirstPerson()) {
                                    flameVec = new Pos3D(1, 1, 1)
                                          .multiply(p.getViewVector(event.renderTickTime))
                                          .yRot(rightHanded ? 15 : -15)
                                          .translate(0, p.getEyeHeight() - 0.1, 0);
                                } else {
                                    double flameXCoord = rightHanded ? -0.2 : 0.2;
                                    double flameYCoord = 1;
                                    double flameZCoord = 1.2;
                                    if (p.isCrouching()) {
                                        flameYCoord -= 0.65;
                                        flameZCoord -= 0.15;
                                    }
                                    flameVec = new Pos3D(flameXCoord, flameYCoord, flameZCoord).yRot(p.yBodyRot);
                                }
                                Vec3 motion = p.getDeltaMovement();
                                Vec3 flameMotion = new Vec3(motion.x(), p.isOnGround() ? 0 : motion.y(), motion.z());
                                Vec3 mergedVec = p.position().add(flameVec);
                                world.addParticle(MekanismParticleTypes.JETPACK_FLAME.get(), mergedVec.x, mergedVec.y, mergedVec.z, flameMotion.x,
                                      flameMotion.y, flameMotion.z);
                            }
                        }
                    }
                }

                if (MekanismAPI.getRadiationManager().isRadiationEnabled() && MekanismUtils.isPlayingMode(player)) {
                    player.getCapability(Capabilities.RADIATION_ENTITY_CAPABILITY).ifPresent(c -> {
                        double radiation = c.getRadiation();
                        double severity = RadiationScale.getScaledDoseSeverity(radiation) * 0.8;
                        if (prevRadiation < severity) {
                            prevRadiation = Math.min(severity, prevRadiation + 0.01);
                        }
                        if (prevRadiation > severity) {
                            prevRadiation = Math.max(severity, prevRadiation - 0.01);
                        }
                        if (severity > RadiationManager.BASELINE) {
                            int effect = (int) (prevRadiation * 255);
                            int color = (0x701E1E << 8) + effect;
                            MekanismRenderer.renderColorOverlay(matrix, 0, 0, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(), color);
                        }
                    });
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockHover(DrawSelectionEvent.HighlightBlock event) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        BlockHitResult rayTraceResult = event.getTarget();
        if (rayTraceResult.getType() != Type.MISS) {
            Level world = player.getCommandSenderWorld();
            BlockPos pos = rayTraceResult.getBlockPos();
            MultiBufferSource renderer = event.getMultiBufferSource();
            Camera info = event.getCamera();
            PoseStack matrix = event.getPoseStack();
            ProfilerFiller profiler = world.getProfiler();
            BlockState blockState = world.getBlockState(pos);

            profiler.push(ProfilerConstants.AREA_MINE_OUTLINE);
            // Draw outlines for area mining blocks
            if (!outliningArea) {
                ItemStack stack = player.getMainHandItem();
                if (!stack.isEmpty() && stack.getItem() instanceof IBlastingItem tool) {
                    Map<BlockPos, BlockState> blocks = tool.getBlastedBlocks(world, player, stack, pos, blockState);
                    if (!blocks.isEmpty()) {
                        outliningArea = true;
                        Vec3 renderView = info.getPosition();
                        LevelRenderer levelRenderer = event.getLevelRenderer();
                        Lazy<VertexConsumer> lineConsumer = Lazy.of(() -> renderer.getBuffer(RenderType.lines()));
                        for (Entry<BlockPos, BlockState> block : blocks.entrySet()) {
                            BlockPos blastingTarget = block.getKey();
                            if (!pos.equals(blastingTarget) && !ForgeHooksClient.onDrawHighlight(levelRenderer, info, rayTraceResult, event.getPartialTicks(), matrix, renderer)) {
                                levelRenderer.renderHitOutline(matrix, lineConsumer.get(), player, renderView.x, renderView.y, renderView.z, blastingTarget, block.getValue());
                            }
                        }
                        outliningArea = false;
                    }
                }
            }
            profiler.pop();

            boolean shouldCancel = false;
            profiler.push(ProfilerConstants.MEKANISM_OUTLINE);
            if (!blockState.isAir() && world.getWorldBorder().isWithinBounds(pos)) {
                BlockPos actualPos = pos;
                BlockState actualState = blockState;
                if (blockState.getBlock() instanceof BlockBounding) {
                    TileEntityBoundingBlock tile = WorldUtils.getTileEntity(TileEntityBoundingBlock.class, world, pos);
                    if (tile != null && tile.hasReceivedCoords()) {
                        actualPos = tile.getMainPos();
                        actualState = world.getBlockState(actualPos);
                    }
                }
                AttributeCustomSelectionBox customSelectionBox = Attribute.get(actualState, AttributeCustomSelectionBox.class);
                if (customSelectionBox != null) {
                    WireFrameRenderer renderWireFrame = null;
                    if (customSelectionBox.isJavaModel()) {
                        //If we use a TER to render the wire frame, grab the tile
                        BlockEntity tile = WorldUtils.getTileEntity(world, actualPos);
                        if (tile != null) {
                            BlockEntityRenderer<BlockEntity> tileRenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(tile);
                            if (tileRenderer instanceof IWireFrameRenderer wireFrameRenderer && wireFrameRenderer.hasSelectionBox(actualState)) {
                                renderWireFrame = (buffer, matrixStack, state, red, green, blue, alpha) -> {
                                    if (wireFrameRenderer.isCombined()) {
                                        renderQuadsWireFrame(state, buffer, matrixStack.last().pose(), world.random, red, green, blue, alpha);
                                    }
                                    wireFrameRenderer.renderWireFrame(tile, event.getPartialTicks(), matrixStack, buffer, red, green, blue, alpha);
                                };
                            }
                        }
                    } else {
                        //Otherwise, skip getting the tile and just grab the model
                        renderWireFrame = (buffer, matrixStack, state, red, green, blue, alpha) ->
                              renderQuadsWireFrame(state, buffer, matrixStack.last().pose(), world.random, red, green, blue, alpha);
                    }
                    if (renderWireFrame != null) {
                        matrix.pushPose();
                        Vec3 viewPosition = info.getPosition();
                        matrix.translate(actualPos.getX() - viewPosition.x, actualPos.getY() - viewPosition.y, actualPos.getZ() - viewPosition.z);
                        renderWireFrame.render(renderer.getBuffer(RenderType.lines()), matrix, actualState, 0, 0, 0, 0.4F);
                        matrix.popPose();
                        shouldCancel = true;
                    }
                }
            }
            profiler.pop();

            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemConfigurator)) {
                //If we are not holding a configurator, look if we are in the offhand
                stack = player.getOffhandItem();
                if (stack.isEmpty() || !(stack.getItem() instanceof ItemConfigurator)) {
                    if (shouldCancel) {
                        event.setCanceled(true);
                    }
                    return;
                }
            }
            profiler.push(ProfilerConstants.CONFIGURABLE_MACHINE);
            ConfiguratorMode state = ((ItemConfigurator) stack.getItem()).getMode(stack);
            if (state.isConfigurating()) {
                TransmissionType type = Objects.requireNonNull(state.getTransmission(), "Configurating state requires transmission type");
                BlockEntity tile = WorldUtils.getTileEntity(world, pos);
                if (tile instanceof ISideConfiguration configurable) {
                    TileComponentConfig config = configurable.getConfig();
                    if (config.supports(type)) {
                        Direction face = rayTraceResult.getDirection();
                        DataType dataType = config.getDataType(type, RelativeSide.fromDirections(configurable.getDirection(), face));
                        if (dataType != null) {
                            Vec3 viewPosition = info.getPosition();
                            matrix.pushPose();
                            matrix.translate(pos.getX() - viewPosition.x, pos.getY() - viewPosition.y, pos.getZ() - viewPosition.z);
                            MekanismRenderer.renderObject(getOverlayModel(face, type), matrix, renderer.getBuffer(Sheets.translucentCullBlockSheet()),
                                  MekanismRenderer.getColorARGB(dataType.getColor(), 0.6F), MekanismRenderer.FULL_LIGHT, OverlayTexture.NO_OVERLAY, FaceDisplay.FRONT);
                            matrix.popPose();
                        }
                    }
                }
            }
            profiler.pop();
            if (shouldCancel) {
                event.setCanceled(true);
            }
        }
    }

    private void renderQuadsWireFrame(BlockState state, VertexConsumer buffer, Matrix4f matrix, Random rand, float red, float green, float blue, float alpha) {
        List<Vertex[]> allVertices = cachedWireFrames.computeIfAbsent(state, s -> {
            BakedModel bakedModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(s);
            //TODO: Eventually we may want to add support for Model data
            IModelData modelData = EmptyModelData.INSTANCE;
            List<Vertex[]> vertices = new ArrayList<>();
            for (Direction direction : EnumUtils.DIRECTIONS) {
                QuadUtils.unpack(bakedModel.getQuads(s, direction, rand, modelData)).stream().map(Quad::getVertices).forEach(vertices::add);
            }
            QuadUtils.unpack(bakedModel.getQuads(s, null, rand, modelData)).stream().map(Quad::getVertices).forEach(vertices::add);
            return vertices;
        });
        renderVertexWireFrame(allVertices, buffer, matrix, red, green, blue, alpha);
    }

    public static void renderVertexWireFrame(List<Vertex[]> allVertices, VertexConsumer buffer, Matrix4f matrix, float red, float green, float blue, float alpha) {
        for (Vertex[] vertices : allVertices) {
            Vector4f vertex = getVertex(matrix, vertices[0]);
            Vec3 normal = vertices[0].getNormal();
            Vector4f vertex2 = getVertex(matrix, vertices[1]);
            Vec3 normal2 = vertices[1].getNormal();
            Vector4f vertex3 = getVertex(matrix, vertices[2]);
            Vec3 normal3 = vertices[2].getNormal();
            Vector4f vertex4 = getVertex(matrix, vertices[3]);
            Vec3 normal4 = vertices[3].getNormal();
            buffer.vertex(vertex.x(), vertex.y(), vertex.z()).color(red, green, blue, alpha).normal((float) normal.x(), (float) normal.y(), (float) normal.z()).endVertex();
            buffer.vertex(vertex2.x(), vertex2.y(), vertex2.z()).color(red, green, blue, alpha).normal((float) normal2.x(), (float) normal2.y(), (float) normal2.z()).endVertex();

            buffer.vertex(vertex3.x(), vertex3.y(), vertex3.z()).color(red, green, blue, alpha).normal((float) normal3.x(), (float) normal3.y(), (float) normal3.z()).endVertex();
            buffer.vertex(vertex4.x(), vertex4.y(), vertex4.z()).color(red, green, blue, alpha).normal((float) normal4.x(), (float) normal4.y(), (float) normal4.z()).endVertex();

            buffer.vertex(vertex2.x(), vertex2.y(), vertex2.z()).color(red, green, blue, alpha).normal((float) normal2.x(), (float) normal2.y(), (float) normal2.z()).endVertex();
            buffer.vertex(vertex3.x(), vertex3.y(), vertex3.z()).color(red, green, blue, alpha).normal((float) normal3.x(), (float) normal3.y(), (float) normal3.z()).endVertex();

            buffer.vertex(vertex.x(), vertex.y(), vertex.z()).color(red, green, blue, alpha).normal((float) normal.x(), (float) normal.y(), (float) normal.z()).endVertex();
            buffer.vertex(vertex4.x(), vertex4.y(), vertex4.z()).color(red, green, blue, alpha).normal((float) normal4.x(), (float) normal4.y(), (float) normal4.z()).endVertex();
        }
    }

    private static Vector4f getVertex(Matrix4f matrix4f, Vertex vertex) {
        Vector4f vector4f = new Vector4f((float) vertex.getPos().x(), (float) vertex.getPos().y(), (float) vertex.getPos().z(), 1);
        vector4f.transform(matrix4f);
        return vector4f;
    }

    private void renderStatusBar(PoseStack matrix, @Nonnull Player player) {
        //TODO: use vanilla status bar text? Note, the vanilla status bar text stays a lot longer than we have our message
        // display for, so we would need to somehow modify it. This can be done via ATs but does cause it to always appear
        // to be more faded in color, and blinks to full color just before disappearing
        if (modeSwitchTimer > 1) {
            if (minecraft.screen == null && minecraft.font != null) {
                ItemStack stack = player.getMainHandItem();
                if (IModeItem.isModeItem(stack, EquipmentSlot.MAINHAND)) {
                    Component scrollTextComponent = ((IModeItem) stack.getItem()).getScrollTextComponent(stack);
                    if (scrollTextComponent != null) {
                        int x = minecraft.getWindow().getGuiScaledWidth();
                        int y = minecraft.getWindow().getGuiScaledHeight();
                        int color = Color.rgbad(1, 1, 1, modeSwitchTimer / 100F).argb();
                        minecraft.font.draw(matrix, scrollTextComponent, (x - minecraft.font.width(scrollTextComponent)) / 2, y - 60, color);
                    }
                }
            }
            modeSwitchTimer--;
        }
    }

    private void renderJetpackSmoke(Level world, Vec3 pos, Vec3 motion) {
        world.addParticle(MekanismParticleTypes.JETPACK_FLAME.get(), pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
        world.addParticle(MekanismParticleTypes.JETPACK_SMOKE.get(), pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
    }

    private Model3D getOverlayModel(Direction side, TransmissionType type) {
        if (cachedOverlays.containsKey(side) && cachedOverlays.get(side).containsKey(type)) {
            return cachedOverlays.get(side).get(type);
        }
        Model3D toReturn = new Model3D();
        toReturn.setTexture(MekanismRenderer.overlays.get(type));
        MekanismRenderer.prepSingleFaceModelSize(toReturn, side);
        cachedOverlays.computeIfAbsent(side, s -> new EnumMap<>(TransmissionType.class)).put(type, toReturn);
        return toReturn;
    }

    @FunctionalInterface
    private interface WireFrameRenderer {

        void render(VertexConsumer buffer, PoseStack matrix, BlockState state, float red, float green, float blue, float alpha);
    }
}