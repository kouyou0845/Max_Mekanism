package mekanism.common.inventory.container.tile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.common.inventory.container.QIOItemViewerContainer;
import mekanism.common.registries.MekanismContainerTypes;
import mekanism.common.tile.qio.TileEntityQIODashboard;
import mekanism.common.util.WorldUtils;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class QIODashboardContainer extends QIOItemViewerContainer {

    private final TileEntityQIODashboard tile;

    public QIODashboardContainer(int id, Inventory inv, TileEntityQIODashboard tile, boolean remote) {
        super(MekanismContainerTypes.QIO_DASHBOARD, id, inv, remote, tile);
        this.tile = tile;
        tile.addContainerTrackers(this);
        addSlotsAndOpen();
    }

    @Override
    public QIODashboardContainer recreate() {
        QIODashboardContainer container = new QIODashboardContainer(containerId, inv, tile, true);
        sync(container);
        return container;
    }

    @Override
    protected void openInventory(@Nonnull Inventory inv) {
        super.openInventory(inv);
        tile.open(inv.player);
    }

    @Override
    protected void closeInventory(@Nonnull Player player) {
        super.closeInventory(player);
        tile.close(player);
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        //prevent Containers from remaining valid after the chunk has unloaded;
        return tile.hasGui() && !tile.isRemoved() && WorldUtils.isBlockLoaded(tile.getLevel(), tile.getBlockPos());
    }

    public TileEntityQIODashboard getTileEntity() {
        return tile;
    }

    @Nullable
    @Override
    public ICapabilityProvider getSecurityObject() {
        return tile;
    }
}
