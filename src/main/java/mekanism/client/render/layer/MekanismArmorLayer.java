package mekanism.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.client.render.armor.ICustomArmor;
import mekanism.client.render.armor.ISpecialGear;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.RenderProperties;

@ParametersAreNonnullByDefault
public class MekanismArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends HumanoidArmorLayer<T, M, A> {

    public MekanismArmorLayer(RenderLayerParent<T, M> entityRenderer, A modelLeggings, A modelArmor) {
        super(entityRenderer, modelLeggings, modelArmor);
    }

    @Override
    public void render(PoseStack matrix, MultiBufferSource renderer, int packedLightIn, T entity, float limbSwing, float limbSwingAmount, float partialTicks,
          float ageInTicks, float netHeadYaw, float headPitch) {
        renderArmorPart(matrix, renderer, entity, EquipmentSlot.CHEST, packedLightIn, partialTicks);
        renderArmorPart(matrix, renderer, entity, EquipmentSlot.LEGS, packedLightIn, partialTicks);
        renderArmorPart(matrix, renderer, entity, EquipmentSlot.FEET, packedLightIn, partialTicks);
        renderArmorPart(matrix, renderer, entity, EquipmentSlot.HEAD, packedLightIn, partialTicks);
    }

    private void renderArmorPart(PoseStack matrix, MultiBufferSource renderer, T entity, EquipmentSlot slot, int light, float partialTicks) {
        ItemStack stack = entity.getItemBySlot(slot);
        Item item = stack.getItem();
        if (item instanceof ArmorItem armorItem && armorItem.getSlot() == slot && RenderProperties.get(item) instanceof ISpecialGear specialGear) {
            ICustomArmor model = specialGear.getGearModel(slot);
            A coreModel = slot == EquipmentSlot.LEGS ? innerModel : outerModel;
            getParentModel().copyPropertiesTo(coreModel);
            setPartVisibility(coreModel, slot);
            model.render(coreModel, matrix, renderer, light, OverlayTexture.NO_OVERLAY, partialTicks, stack.hasFoil(), entity, stack);
        }
    }
}