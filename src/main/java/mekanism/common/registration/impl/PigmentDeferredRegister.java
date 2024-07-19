package mekanism.common.registration.impl;

import java.util.function.Supplier;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentBuilder;
import mekanism.common.registration.WrappedForgeDeferredRegister;
import net.minecraft.resources.ResourceLocation;

public class PigmentDeferredRegister extends WrappedForgeDeferredRegister<Pigment> {

    public PigmentDeferredRegister(String modid) {
        super(modid, MekanismAPI.pigmentRegistryName());
    }

    public PigmentRegistryObject<Pigment> register(String name, int tint) {
        return register(name, () -> new Pigment(PigmentBuilder.builder().color(tint)));
    }

    public PigmentRegistryObject<Pigment> register(String name, ResourceLocation texture) {
        return register(name, () -> new Pigment(PigmentBuilder.builder(texture)));
    }

    public <PIGMENT extends Pigment> PigmentRegistryObject<PIGMENT> register(String name, Supplier<? extends PIGMENT> sup) {
        return register(name, sup, PigmentRegistryObject::new);
    }
}