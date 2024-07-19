package mekanism.common.inventory.slot;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.api.annotations.FieldsAreNonnullByDefault;
import mekanism.common.content.qio.QIOCraftingWindow;
import mekanism.common.inventory.container.slot.VirtualCraftingOutputSlot;
import mekanism.common.inventory.container.slot.VirtualInventoryContainerSlot;
import net.minecraft.MethodsReturnNonnullByDefault;

@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CraftingWindowOutputInventorySlot extends CraftingWindowInventorySlot {

    public static CraftingWindowOutputInventorySlot create(QIOCraftingWindow window) {
        return new CraftingWindowOutputInventorySlot(window);
    }

    private CraftingWindowOutputInventorySlot(QIOCraftingWindow window) {
        super(manualOnly, internalOnly, window, null, null);
    }

    @Nonnull
    @Override
    public VirtualInventoryContainerSlot createContainerSlot() {
        return new VirtualCraftingOutputSlot(this, getSlotOverlay(), this::setStackUnchecked, craftingWindow);
    }
}