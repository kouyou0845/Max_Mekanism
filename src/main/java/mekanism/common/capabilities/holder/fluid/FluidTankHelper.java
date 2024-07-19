package mekanism.common.capabilities.holder.fluid;

import java.util.function.Supplier;
import javax.annotation.Nonnull;
import mekanism.api.RelativeSide;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.common.tile.component.TileComponentConfig;
import net.minecraft.core.Direction;

public class FluidTankHelper {

    private final IFluidTankHolder slotHolder;
    private boolean built;

    private FluidTankHelper(IFluidTankHolder slotHolder) {
        this.slotHolder = slotHolder;
    }

    public static FluidTankHelper forSide(Supplier<Direction> facingSupplier) {
        return new FluidTankHelper(new FluidTankHolder(facingSupplier));
    }

    public static FluidTankHelper forSideWithConfig(Supplier<Direction> facingSupplier, Supplier<TileComponentConfig> configSupplier) {
        return new FluidTankHelper(new ConfigFluidTankHolder(facingSupplier, configSupplier));
    }

    public <TANK extends IExtendedFluidTank> TANK addTank(@Nonnull TANK tank) {
        if (built) {
            throw new IllegalStateException("Builder has already built.");
        }
        if (slotHolder instanceof FluidTankHolder slotHolder) {
            slotHolder.addTank(tank);
        } else if (slotHolder instanceof ConfigFluidTankHolder slotHolder) {
            slotHolder.addTank(tank);
        } else {
            throw new IllegalArgumentException("Holder does not know how to add tanks");
        }
        return tank;
    }

    public <TANK extends IExtendedFluidTank> TANK addTank(@Nonnull TANK tank, RelativeSide... sides) {
        if (built) {
            throw new IllegalStateException("Builder has already built.");
        }
        if (slotHolder instanceof FluidTankHolder slotHolder) {
            slotHolder.addTank(tank, sides);
        } else {
            throw new IllegalArgumentException("Holder does not know how to add tanks on specific sides");
        }
        return tank;
    }

    public IFluidTankHolder build() {
        built = true;
        return slotHolder;
    }
}