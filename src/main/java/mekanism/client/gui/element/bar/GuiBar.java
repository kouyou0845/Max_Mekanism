package mekanism.client.gui.element.bar;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.BooleanSupplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiTexturedElement;
import mekanism.client.gui.element.bar.GuiBar.IBarInfoHandler;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.common.inventory.warning.ISupportsWarning;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class GuiBar<INFO extends IBarInfoHandler> extends GuiTexturedElement implements ISupportsWarning<GuiBar<INFO>> {

    public static final ResourceLocation BAR = MekanismUtils.getResource(ResourceType.GUI_BAR, "base.png");

    private final INFO handler;
    protected final boolean horizontal;
    @Nullable
    private BooleanSupplier warningSupplier;

    public GuiBar(ResourceLocation resource, IGuiWrapper gui, INFO handler, int x, int y, int width, int height, boolean horizontal) {
        super(resource, gui, x, y, width + 2, height + 2);
        this.handler = handler;
        this.horizontal = horizontal;
    }

    @Override
    public GuiBar<INFO> warning(@Nonnull WarningType type, @Nonnull BooleanSupplier warningSupplier) {
        this.warningSupplier = ISupportsWarning.compound(this.warningSupplier, gui().trackWarning(type, warningSupplier));
        return this;
    }

    public INFO getHandler() {
        return handler;
    }

    @Override
    public void drawBackground(@Nonnull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        //Render the bar
        renderExtendedTexture(matrix, BAR, 2, 2);
        boolean warning = warningSupplier != null && warningSupplier.getAsBoolean();
        if (warning) {
            //Draw background (we do it regardless of if we are full or not as if the thing being drawn has transparency
            // we may as well show the background)
            RenderSystem.setShaderTexture(0, GuiSlot.WARNING_BACKGROUND_TEXTURE);
            blit(matrix, x + 1, y + 1, 0, 0, width - 2, height - 2, 256, 256);
        }
        //Render Contents
        drawContentsChecked(matrix, mouseX, mouseY, partialTicks, handler.getLevel(), warning);
    }

    void drawContentsChecked(@Nonnull PoseStack matrix, int mouseX, int mouseY, float partialTicks, double handlerLevel, boolean warning) {
        //If there are any contents render them
        if (handlerLevel > 0) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, getResource());
            renderBarOverlay(matrix, mouseX, mouseY, partialTicks, handlerLevel);
            if (warning && handlerLevel >= 0.98) {
                //Greater than 98% filled, render secondary piece anyway just to make it more visible
                RenderSystem.setShaderTexture(0, WARNING_TEXTURE);
                //Note: We also start the drawing after half the dimension so that we are sure it will properly line up with
                // the one drawn to the background if the contents of things are translucent
                if (horizontal) {
                    int halfHeight = (height - 2) / 2;
                    blit(matrix, x + 1, y + 1 + halfHeight, 0, halfHeight, width - 2, halfHeight, 256, 256);
                } else {//vertical
                    int halfWidth = (width - 2) / 2;
                    blit(matrix, x + 1 + halfWidth, y + 1, halfWidth, 0, halfWidth, height - 2, 256, 256);
                }
            }
        }
    }

    protected abstract void renderBarOverlay(PoseStack matrix, int mouseX, int mouseY, float partialTicks, double handlerLevel);

    @Override
    public void renderToolTip(@Nonnull PoseStack matrix, int mouseX, int mouseY) {
        super.renderToolTip(matrix, mouseX, mouseY);
        Component tooltip = handler.getTooltip();
        if (tooltip != null) {
            displayTooltips(matrix, mouseX, mouseY, tooltip);
        }
    }

    protected static int calculateScaled(double scale, int value) {
        if (scale == 1) {
            return value;
        } else if (scale < 1) {
            //Round down
            return (int) (scale * value);
        }//else > 1
        //Allow rounding up
        return (int) Math.round(scale * value);
    }

    public interface IBarInfoHandler {

        @Nullable
        default Component getTooltip() {
            return null;
        }

        double getLevel();
    }
}