package mekanism.additions.common.block.plastic;

import javax.annotation.Nonnull;
import mekanism.api.text.EnumColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockPlasticRoad extends BlockPlastic {

    public BlockPlasticRoad(EnumColor color) {
        super(color, properties -> properties.strength(5, 6));
    }

    @Override
    public void stepOn(@Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState state, Entity entity) {
        double boost = 1.6;
        Vec3 motion = entity.getDeltaMovement();
        double a = Math.atan2(motion.x(), motion.z());
        float slipperiness = state.getFriction(world, pos, entity);
        motion = motion.add(Math.sin(a) * boost * slipperiness, 0, Math.cos(a) * boost * slipperiness);
        entity.setDeltaMovement(motion);
    }
}