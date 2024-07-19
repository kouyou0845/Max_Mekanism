package mekanism.client.gui.element.window.filter.miner;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.window.filter.GuiModIDFilter;
import mekanism.common.base.TagCache;
import mekanism.common.content.miner.MinerModIDFilter;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import net.minecraft.world.item.ItemStack;

public class GuiMinerModIDFilter extends GuiModIDFilter<MinerModIDFilter, TileEntityDigitalMiner> implements GuiMinerFilterHelper {

    public static GuiMinerModIDFilter create(IGuiWrapper gui, TileEntityDigitalMiner tile) {
        return new GuiMinerModIDFilter(gui, (gui.getWidth() - 173) / 2, 30, tile, null);
    }

    public static GuiMinerModIDFilter edit(IGuiWrapper gui, TileEntityDigitalMiner tile, MinerModIDFilter filter) {
        return new GuiMinerModIDFilter(gui, (gui.getWidth() - 173) / 2, 30, tile, filter);
    }

    private GuiMinerModIDFilter(IGuiWrapper gui, int x, int y, TileEntityDigitalMiner tile, @Nullable MinerModIDFilter origFilter) {
        super(gui, x, y, 173, 90, tile, origFilter);
    }

    @Override
    protected void init() {
        super.init();
        addMinerDefaults(gui(), filter, getSlotOffset(), this::addChild);
    }

    @Override
    protected MinerModIDFilter createNewFilter() {
        return new MinerModIDFilter();
    }

    @Nonnull
    @Override
    protected List<ItemStack> getRenderStacks() {
        if (filter.hasFilter()) {
            return TagCache.getBlockModIDStacks(filter.getModID()).stacks();
        }
        return Collections.emptyList();
    }

    @Override
    protected boolean hasMatchingTargets(String name) {
        return TagCache.getBlockModIDStacks(name).hasMatch();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return tryClickReplaceStack(gui(), mouseX, mouseY, button, getSlotOffset(), filter) || super.mouseClicked(mouseX, mouseY, button);
    }
}