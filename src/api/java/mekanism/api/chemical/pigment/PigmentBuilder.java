package mekanism.api.chemical.pigment;

import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PigmentBuilder extends ChemicalBuilder<Pigment, PigmentBuilder> {

    protected PigmentBuilder(ResourceLocation texture) {
        super(texture);
    }

    /**
     * Creates a builder for registering a {@link Pigment}, using our default {@link Pigment} texture.
     *
     * @return A builder for creating a {@link Pigment}.
     */
    public static PigmentBuilder builder() {
        return builder(new ResourceLocation(MekanismAPI.MEKANISM_MODID, "pigment/base"));
    }

    /**
     * Creates a builder for registering a {@link Pigment}, with a given texture.
     *
     * @param texture A {@link ResourceLocation} representing the texture this {@link Pigment} will use.
     *
     * @return A builder for creating a {@link Pigment}.
     *
     * @apiNote The texture will be automatically stitched to the block texture atlas.
     * <br>
     * It is recommended to override {@link Pigment#getColorRepresentation()} if this builder method is not used in combination with {@link #color(int)} due to the
     * texture not needing tinting.
     */
    public static PigmentBuilder builder(ResourceLocation texture) {
        return new PigmentBuilder(Objects.requireNonNull(texture));
    }
}