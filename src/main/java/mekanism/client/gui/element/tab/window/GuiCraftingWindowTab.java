package mekanism.client.gui.element.tab.window;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import mekanism.client.SpecialColors;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.window.GuiCraftingWindow;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.MekanismLang;
import mekanism.common.content.qio.IQIOCraftingWindowHolder;
import mekanism.common.inventory.container.QIOItemViewerContainer;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;

public class GuiCraftingWindowTab extends GuiWindowCreatorTab<Void, GuiCraftingWindowTab> {

    private final boolean[] openWindows = new boolean[IQIOCraftingWindowHolder.MAX_CRAFTING_WINDOWS];
    private final QIOItemViewerContainer container;
    private byte currentWindows;

    public GuiCraftingWindowTab(IGuiWrapper gui, Supplier<GuiCraftingWindowTab> elementSupplier, QIOItemViewerContainer container) {
        super(MekanismUtils.getResource(ResourceType.GUI_BUTTON, "crafting.png"), gui, null, -26, 34, 26, 18, true, elementSupplier);
        this.container = container;
    }

    @Override
    public void renderToolTip(@Nonnull PoseStack matrix, int mouseX, int mouseY) {
        super.renderToolTip(matrix, mouseX, mouseY);
        displayTooltips(matrix, mouseX, mouseY, MekanismLang.CRAFTING_TAB.translate(currentWindows, IQIOCraftingWindowHolder.MAX_CRAFTING_WINDOWS));
    }

    @Override
    protected void colorTab() {
        MekanismRenderer.color(SpecialColors.TAB_CRAFTING_WINDOW.get());
    }

    @Override
    protected Consumer<GuiWindow> getCloseListener() {
        return window -> {
            GuiCraftingWindowTab tab = getElementSupplier().get();
            if (window instanceof GuiCraftingWindow craftingWindow) {
                tab.openWindows[craftingWindow.getIndex()] = false;
            }
            tab.currentWindows--;
            if (tab.currentWindows < IQIOCraftingWindowHolder.MAX_CRAFTING_WINDOWS) {
                //If we have less than the max number of windows re-enable the tab
                tab.active = true;
            }
        };
    }

    @Override
    protected Consumer<GuiWindow> getReAttachListener() {
        return super.getReAttachListener().andThen(window -> {
            if (window instanceof GuiCraftingWindow craftingWindow) {
                GuiCraftingWindowTab tab = getElementSupplier().get();
                tab.openWindows[craftingWindow.getIndex()] = true;
            }
        });
    }

    @Override
    protected void disableTab() {
        currentWindows++;
        if (currentWindows >= IQIOCraftingWindowHolder.MAX_CRAFTING_WINDOWS) {
            //If we have the max number of windows we are allowed then disable the tab
            super.disableTab();
        }
    }

    @Override
    protected GuiWindow createWindow() {
        byte index = 0;
        for (int i = 0; i < openWindows.length; i++) {
            if (!openWindows[i]) {
                //Note: We cast it to a byte as it realistically will never be more than 2
                index = (byte) i;
                break;
            }
        }
        openWindows[index] = true;
        return new GuiCraftingWindow(gui(), getGuiWidth() / 2 - 156 / 2, 15, container, index);
    }
}