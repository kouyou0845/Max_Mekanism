package mekanism.additions.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Random;
import javax.annotation.Nonnull;
import mekanism.additions.client.model.ModelBabyEnderman;
import mekanism.additions.client.render.entity.layer.BabyEndermanEyesLayer;
import mekanism.additions.client.render.entity.layer.BabyEndermanHeldBlockLayer;
import mekanism.additions.common.entity.baby.EntityBabyEnderman;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Copy of vanilla's enderman render, modified to use our own model/layer that is properly scaled, so that the block is held in the correct spot and the head is in the
 * proper place.
 */
public class RenderBabyEnderman extends MobRenderer<EntityBabyEnderman, ModelBabyEnderman> {

    private static final ResourceLocation ENDERMAN_TEXTURES = new ResourceLocation("textures/entity/enderman/enderman.png");
    private final Random rnd = new Random();

    public RenderBabyEnderman(EntityRendererProvider.Context context) {
        super(context, new ModelBabyEnderman(context.bakeLayer(ModelLayers.ENDERMAN)), 0.5F);
        this.addLayer(new BabyEndermanEyesLayer(this));
        this.addLayer(new BabyEndermanHeldBlockLayer(this));
    }

    @Override
    public void render(EntityBabyEnderman enderman, float entityYaw, float partialTicks, @Nonnull PoseStack matrix, @Nonnull MultiBufferSource renderer, int packedLightIn) {
        ModelBabyEnderman model = getModel();
        model.carrying = enderman.getCarriedBlock() != null;
        model.creepy = enderman.isCreepy();
        super.render(enderman, entityYaw, partialTicks, matrix, renderer, packedLightIn);
    }

    @Nonnull
    @Override
    public Vec3 getRenderOffset(EntityBabyEnderman enderman, float partialTicks) {
        if (enderman.isCreepy()) {
            return new Vec3(this.rnd.nextGaussian() * 0.02, 0, this.rnd.nextGaussian() * 0.02);
        }
        return super.getRenderOffset(enderman, partialTicks);
    }

    @Nonnull
    @Override
    public ResourceLocation getTextureLocation(@Nonnull EntityBabyEnderman enderman) {
        return ENDERMAN_TEXTURES;
    }
}