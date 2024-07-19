package mekanism.common.integration.crafttweaker.content.builder;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.tag.type.KnownTag;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryBuilder;
import mekanism.common.integration.crafttweaker.CrTConstants;
import mekanism.common.integration.crafttweaker.content.CrTContentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister(loaders = CrTConstants.CONTENT_LOADER)
@ZenCodeType.Name(CrTConstants.CLASS_BUILDER_SLURRY)
public class CrTSlurryBuilder extends CrTChemicalBuilder<Slurry, SlurryBuilder, CrTSlurryBuilder> {

    /**
     * Creates a builder for registering a custom {@link Slurry}, using our default clean {@link Slurry} texture.
     *
     * @return A builder for creating a custom {@link Slurry}.
     */
    @ZenCodeType.Method
    public static CrTSlurryBuilder clean() {
        return new CrTSlurryBuilder(SlurryBuilder.clean());
    }

    /**
     * Creates a builder for registering a custom {@link Slurry}, using our default dirty {@link Slurry} texture.
     *
     * @return A builder for creating a custom {@link Slurry}.
     */
    @ZenCodeType.Method
    public static CrTSlurryBuilder dirty() {
        return new CrTSlurryBuilder(SlurryBuilder.dirty());
    }

    /**
     * Creates a builder for registering a custom {@link Slurry}.
     *
     * @param textureLocation A {@link ResourceLocation} representing the texture this {@link Slurry} will use.
     *
     * @return A builder for creating a custom {@link Slurry}.
     *
     * @apiNote It is recommended to override to use {@link #colorRepresentation(int)} if this builder method is not being used in combination with {@link #color(int)}
     * due to the texture not needing tinting.
     */
    @ZenCodeType.Method
    public static CrTSlurryBuilder builder(ResourceLocation textureLocation) {
        return new CrTSlurryBuilder(SlurryBuilder.builder(textureLocation));
    }

    protected CrTSlurryBuilder(SlurryBuilder builder) {
        super(builder);
    }

    /**
     * Sets the tag that represents the ore that goes with this {@link Slurry}.
     *
     * @param oreTagLocation {@link ResourceLocation} of the item tag representing the ore.
     */
    @ZenCodeType.Method
    public CrTSlurryBuilder ore(ResourceLocation oreTagLocation) {
        getInternal().ore(oreTagLocation);
        return this;
    }

    /**
     * Sets the tag that represents the ore that goes with this {@link Slurry}.
     *
     * @param oreTag Tag representing the ore.
     *
     * @implNote We add the tag by its internal id instead of getting the internal tag as the internal tag would currently be {@code null} when this gets called, as the
     * collection is empty, and the goal of this method is to let the slurry have a {@link net.minecraft.tags.TagKey}
     */
    @ZenCodeType.Method
    public CrTSlurryBuilder ore(KnownTag<Item> oreTag) {
        getInternal().ore(oreTag.getTagKey());
        return this;
    }

    @Override
    protected void build(ResourceLocation registryName) {
        Slurry slurry;
        if (colorRepresentation == null) {
            slurry = new Slurry(getInternal());
        } else {
            int color = colorRepresentation;
            slurry = new Slurry(getInternal()) {
                @Override
                public int getColorRepresentation() {
                    return color;
                }
            };
        }
        CrTContentUtils.queueSlurryForRegistration(registryName, slurry);
    }
}