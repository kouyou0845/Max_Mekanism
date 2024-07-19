package mekanism.client.model;

import com.google.common.collect.Table.Cell;
import mekanism.common.Mekanism;
import mekanism.common.registration.impl.ItemRegistryObject;
import mekanism.common.registries.MekanismFluids;
import mekanism.common.registries.MekanismItems;
import mekanism.common.resource.PrimaryResource;
import mekanism.common.resource.ResourceType;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;

public class MekanismItemModelProvider extends BaseItemModelProvider {

    public MekanismItemModelProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, Mekanism.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        registerBuckets(MekanismFluids.FLUIDS);
        registerModules(MekanismItems.ITEMS);
        for (Cell<ResourceType, PrimaryResource, ItemRegistryObject<Item>> item : MekanismItems.PROCESSED_RESOURCES.cellSet()) {
            ResourceLocation texture = itemTexture(item.getValue());
            if (textureExists(texture)) {
                generated(item.getValue(), texture);
            } else {
                //If the texture does not exist fallback to the default texture
                resource(item.getValue(), item.getRowKey().getRegistryPrefix());
            }
        }
    }
}