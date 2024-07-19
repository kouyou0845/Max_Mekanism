package mekanism.common.capabilities.resolver.manager;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.api.annotations.FieldsAreNonnullByDefault;
import mekanism.common.capabilities.holder.IHolder;
import mekanism.common.capabilities.resolver.BasicSidedCapabilityResolver;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CapabilityHandlerManager<HOLDER extends IHolder, CONTAINER, HANDLER, SIDED_HANDLER extends HANDLER> extends BasicSidedCapabilityResolver<HANDLER, SIDED_HANDLER>
      implements ICapabilityHandlerManager<CONTAINER> {

    private final BiFunction<HOLDER, Direction, List<CONTAINER>> containerGetter;
    private final boolean canHandle;
    @Nullable
    protected final HOLDER holder;

    protected CapabilityHandlerManager(@Nullable HOLDER holder, SIDED_HANDLER baseHandler, Capability<HANDLER> supportedCapability,
          ProxyCreator<HANDLER, SIDED_HANDLER> proxyCreator, BiFunction<HOLDER, Direction, List<CONTAINER>> containerGetter) {
        super(baseHandler, supportedCapability, proxyCreator, holder != null);
        this.holder = holder;
        this.canHandle = this.holder != null;
        this.containerGetter = containerGetter;
    }

    @Override
    public boolean canHandle() {
        return canHandle;
    }

    @Override
    public List<CONTAINER> getContainers(@Nullable Direction side) {
        return canHandle() ? containerGetter.apply(holder, side) : Collections.emptyList();
    }

    @Nullable
    @Override
    protected IHolder getHolder() {
        return holder;
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote Assumes that {@link #canHandle} has been called before this and that it was {@code true}.
     */
    @Override
    public <T> LazyOptional<T> resolve(Capability<T> capability, @Nullable Direction side) {
        if (getContainers(side).isEmpty()) {
            //If we don't have any containers accessible from that side, don't return a handler
            //TODO: Evaluate moving this somehow into being done via the is disabled check
            return LazyOptional.empty();
        }
        return super.resolve(capability, side);
    }
}