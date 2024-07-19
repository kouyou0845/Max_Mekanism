package mekanism.client.gui.element.window.filter.miner;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.window.filter.GuiTagFilter;
import mekanism.common.base.TagCache;
import mekanism.common.content.miner.MinerTagFilter;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import net.minecraft.world.item.ItemStack;

public class GuiMinerTagFilter extends GuiTagFilter<MinerTagFilter, TileEntityDigitalMiner> implements GuiMinerFilterHelper {

    public static GuiMinerTagFilter create(IGuiWrapper gui, TileEntityDigitalMiner tile) {
        return new GuiMinerTagFilter(gui, (gui.getWidth() - 173) / 2, 30, tile, null);
    }

    public static GuiMinerTagFilter edit(IGuiWrapper gui, TileEntityDigitalMiner tile, MinerTagFilter filter) {
        return new GuiMinerTagFilter(gui, (gui.getWidth() - 173) / 2, 30, tile, filter);
    }

    private GuiMinerTagFilter(IGuiWrapper gui, int x, int y, TileEntityDigitalMiner tile, @Nullable MinerTagFilter origFilter) {
        super(gui, x, y, 173, 90, tile, origFilter);
    }

    @Override
    protected void init() {
        super.init();
        addMinerDefaults(gui(), filter, getSlotOffset(), this::addChild);
    }

    @Override
    protected MinerTagFilter createNewFilter() {
        return new MinerTagFilter();
    }

    @Nonnull
    @Override
    protected List<ItemStack> getRenderStacks() {
        if (filter.hasFilter()) {
            return TagCache.getBlockTagStacks(filter.getTagName()).stacks();
        }
        return Collections.emptyList();
    }

    @Override
    protected boolean hasMatchingTargets(String name) {
        return TagCache.getBlockTagStacks(name).hasMatch();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return tryClickReplaceStack(gui(), mouseX, mouseY, button, getSlotOffset(), filter) || super.mouseClicked(mouseX, mouseY, button);
    }
}