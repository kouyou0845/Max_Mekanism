package mekanism.common.registration.impl;

import java.util.function.Supplier;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfuseTypeBuilder;
import mekanism.common.registration.WrappedForgeDeferredRegister;
import net.minecraft.resources.ResourceLocation;

public class InfuseTypeDeferredRegister extends WrappedForgeDeferredRegister<InfuseType> {

    public InfuseTypeDeferredRegister(String modid) {
        super(modid, MekanismAPI.infuseTypeRegistryName());
    }

    public InfuseTypeRegistryObject<InfuseType> register(String name, int tint) {
        return register(name, () -> new InfuseType(InfuseTypeBuilder.builder().color(tint)));
    }

    public InfuseTypeRegistryObject<InfuseType> register(String name, ResourceLocation texture, int barColor) {
        return register(name, () -> new InfuseType(InfuseTypeBuilder.builder(texture)) {
            @Override
            public int getColorRepresentation() {
                return barColor;
            }
        });
    }

    public <INFUSE_TYPE extends InfuseType> InfuseTypeRegistryObject<INFUSE_TYPE> register(String name, Supplier<? extends INFUSE_TYPE> sup) {
        return register(name, sup, InfuseTypeRegistryObject::new);
    }
}