package mekanism.common.capabilities.proxy;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.api.IConfigurable;
import mekanism.api.annotations.FieldsAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ProxyConfigurable extends ProxyHandler implements IConfigurable {

    private final ISidedConfigurable configurable;

    public ProxyConfigurable(ISidedConfigurable configurable, @Nullable Direction side) {
        super(side, null);
        this.configurable = configurable;
    }

    @Override
    public InteractionResult onSneakRightClick(Player player) {
        return readOnly || side == null ? InteractionResult.PASS : configurable.onSneakRightClick(player, side);
    }

    @Override
    public InteractionResult onRightClick(Player player) {
        return readOnly || side == null ? InteractionResult.PASS : configurable.onRightClick(player, side);
    }

    public interface ISidedConfigurable extends IConfigurable {

        InteractionResult onSneakRightClick(Player player, Direction side);

        @Override
        default InteractionResult onSneakRightClick(Player player) {
            return InteractionResult.PASS;
        }

        InteractionResult onRightClick(Player player, Direction side);

        @Override
        default InteractionResult onRightClick(Player player) {
            return InteractionResult.PASS;
        }
    }
}