package mekanism.client.gui.element.gauge;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import mekanism.api.chemical.merged.MergedChemicalTank;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.jei.interfaces.IJEIIngredientHelper;
import mekanism.common.capabilities.chemical.dynamic.IGasTracker;
import mekanism.common.capabilities.chemical.dynamic.IInfusionTracker;
import mekanism.common.capabilities.chemical.dynamic.IPigmentTracker;
import mekanism.common.capabilities.chemical.dynamic.ISlurryTracker;
import mekanism.common.lib.transmitter.TransmissionType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;

public class GuiMergedChemicalTankGauge<HANDLER extends IGasTracker & IInfusionTracker & IPigmentTracker & ISlurryTracker> extends GuiGauge<Void>
      implements IJEIIngredientHelper {

    private final Supplier<MergedChemicalTank> mergedTankSupplier;
    private final Supplier<HANDLER> handlerSupplier;

    private final GuiGasGauge gasGauge;
    private final GuiInfusionGauge infusionGauge;
    private final GuiPigmentGauge pigmentGauge;
    private final GuiSlurryGauge slurryGauge;

    private Component label;

    public GuiMergedChemicalTankGauge(Supplier<MergedChemicalTank> mergedTankSupplier, Supplier<HANDLER> handlerSupplier, GaugeType type, IGuiWrapper gui, int x, int y) {
        this(mergedTankSupplier, handlerSupplier, type, gui, x, y, type.getGaugeOverlay().getWidth() + 2, type.getGaugeOverlay().getHeight() + 2);
    }

    public GuiMergedChemicalTankGauge(Supplier<MergedChemicalTank> mergedTankSupplier, Supplier<HANDLER> handlerSupplier, GaugeType type, IGuiWrapper gui, int x, int y,
          int width, int height) {
        super(type, gui, x, y, width, height);
        this.mergedTankSupplier = mergedTankSupplier;
        this.handlerSupplier = handlerSupplier;
        gasGauge = addPositionOnlyChild(new GuiGasGauge(() -> this.mergedTankSupplier.get().getGasTank(), () -> this.handlerSupplier.get().getGasTanks(null), type, gui, x, y, width, height));
        infusionGauge = addPositionOnlyChild(new GuiInfusionGauge(() -> this.mergedTankSupplier.get().getInfusionTank(), () -> this.handlerSupplier.get().getInfusionTanks(null), type, gui, x, y, width, height));
        pigmentGauge = addPositionOnlyChild(new GuiPigmentGauge(() -> this.mergedTankSupplier.get().getPigmentTank(), () -> this.handlerSupplier.get().getPigmentTanks(null), type, gui, x, y, width, height));
        slurryGauge = addPositionOnlyChild(new GuiSlurryGauge(() -> this.mergedTankSupplier.get().getSlurryTank(), () -> this.handlerSupplier.get().getSlurryTanks(null), type, gui, x, y, width, height));
    }

    public GuiMergedChemicalTankGauge<HANDLER> setLabel(Component label) {
        this.label = label;
        return this;
    }

    @Override
    public GaugeOverlay getGaugeOverlay() {
        return getCurrentGauge().getGaugeOverlay();
    }

    @Override
    protected GaugeInfo getGaugeColor() {
        return getCurrentGauge().getGaugeColor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        GuiTankGauge<?, ?> currentGauge = getCurrentGaugeNoFallback();
        if (currentGauge == null) {
            //If all the tanks are currently empty, pass the click event to all of them;
            // if multiple types are somehow stored in the dropper, insertion checks should prevent them from being inserted at the same time
            return gasGauge.mouseClicked(mouseX, mouseY, button) | infusionGauge.mouseClicked(mouseX, mouseY, button) |
                   pigmentGauge.mouseClicked(mouseX, mouseY, button) | slurryGauge.mouseClicked(mouseX, mouseY, button);
        }
        //Otherwise, just send the click event to the corresponding gauge
        return currentGauge.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void applyRenderColor() {
        GuiTankGauge<?, ?> currentGauge = getCurrentGaugeNoFallback();
        if (currentGauge != null) {
            currentGauge.applyRenderColor();
        }
    }

    @Nullable
    @Override
    public Object getIngredient(double mouseX, double mouseY) {
        GuiTankGauge<?, ?> currentGauge = getCurrentGaugeNoFallback();
        return currentGauge == null ? null : currentGauge.getIngredient(mouseX, mouseY);
    }

    @Override
    public int getScaledLevel() {
        GuiTankGauge<?, ?> currentGauge = getCurrentGaugeNoFallback();
        return currentGauge == null ? 0 : currentGauge.getScaledLevel();
    }

    @Override
    public TextureAtlasSprite getIcon() {
        return getCurrentGauge().getIcon();
    }

    @Override
    public List<Component> getTooltipText() {
        return getCurrentGauge().getTooltipText();
    }

    @Override
    public Component getLabel() {
        return label;
    }

    @Override
    public TransmissionType getTransmission() {
        return getCurrentGauge().getTransmission();
    }

    private GuiTankGauge<?, ?> getCurrentGauge() {
        GuiTankGauge<?, ?> currentGauge = getCurrentGaugeNoFallback();
        //Fallback to the gas gauge
        return currentGauge == null ? gasGauge : currentGauge;
    }

    @Nullable
    private GuiTankGauge<?, ?> getCurrentGaugeNoFallback() {
        MergedChemicalTank mergedTank = mergedTankSupplier.get();
        return switch (mergedTank.getCurrent()) {
            case GAS -> gasGauge;
            case INFUSION -> infusionGauge;
            case PIGMENT -> pigmentGauge;
            case SLURRY -> slurryGauge;
            default -> null;
        };
    }
}