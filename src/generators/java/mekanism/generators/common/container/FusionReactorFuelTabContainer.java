package mekanism.generators.common.container;

import mekanism.common.inventory.container.tile.EmptyTileContainer;
import mekanism.generators.common.registries.GeneratorsContainerTypes;
import mekanism.generators.common.tile.fusion.TileEntityFusionReactorController;
import net.minecraft.world.entity.player.Inventory;

public class FusionReactorFuelTabContainer extends EmptyTileContainer<TileEntityFusionReactorController> {

    public FusionReactorFuelTabContainer(int id, Inventory inv, TileEntityFusionReactorController tile) {
        super(GeneratorsContainerTypes.FUSION_REACTOR_FUEL, id, inv, tile);
    }

    @Override
    protected void addContainerTrackers() {
        super.addContainerTrackers();
        tile.addFuelTabContainerTrackers(this);
    }
}