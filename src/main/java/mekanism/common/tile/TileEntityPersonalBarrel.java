package mekanism.common.tile;

import javax.annotation.Nonnull;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityPersonalBarrel extends TileEntityPersonalStorage {

    public TileEntityPersonalBarrel(BlockPos pos, BlockState state) {
        super(MekanismBlocks.PERSONAL_BARREL, pos, state);
    }

    @Override
    protected void onOpen(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state) {
        playSound(level, state, SoundEvents.BARREL_OPEN);
        level.setBlockAndUpdate(getBlockPos(), state.setValue(BarrelBlock.OPEN, true));
    }

    @Override
    protected void onClose(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state) {
        playSound(level, state, SoundEvents.BARREL_CLOSE);
        level.setBlockAndUpdate(getBlockPos(), state.setValue(BarrelBlock.OPEN, false));
    }

    private void playSound(@Nonnull Level level, BlockState state, SoundEvent sound) {
        Vec3i vec3i = state.getValue(BarrelBlock.FACING).getNormal();
        double d0 = (double) this.worldPosition.getX() + 0.5D + (double) vec3i.getX() / 2.0D;
        double d1 = (double) this.worldPosition.getY() + 0.5D + (double) vec3i.getY() / 2.0D;
        double d2 = (double) this.worldPosition.getZ() + 0.5D + (double) vec3i.getZ() / 2.0D;
        level.playSound(null, d0, d1, d2, sound, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
    }

    @Override
    protected ResourceLocation getStat() {
        return Stats.OPEN_BARREL;
    }
}