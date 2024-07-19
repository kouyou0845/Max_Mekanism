package mekanism.api.lasers;

import javax.annotation.Nonnull;
import mekanism.api.math.FloatingLong;

/**
 * Expose this as a capability on your TileEntity to handle what happens when a laser hits it.
 */
public interface ILaserReceptor {

    /**
     * Called to receive energy from a laser when the block is hit by a laser.
     *
     * @param energy Energy received.
     */
    void receiveLaserEnergy(@Nonnull FloatingLong energy);

    /**
     * Checks if a laser can break this receptor.
     *
     * @return {@code false} to not allow the laser to break this block.
     */
    boolean canLasersDig();
}