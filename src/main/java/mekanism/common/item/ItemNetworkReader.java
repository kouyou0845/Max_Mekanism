package mekanism.common.item;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.MekanismAPI;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.heat.IHeatHandler;
import mekanism.api.math.FloatingLong;
import mekanism.api.text.EnumColor;
import mekanism.api.text.ILangEntry;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.MekanismLang;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.lib.transmitter.DynamicNetwork;
import mekanism.common.lib.transmitter.TransmitterNetworkRegistry;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import mekanism.common.util.CapabilityUtils;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.StorageUtils;
import mekanism.common.util.UnitDisplayUtils.TemperatureUnit;
import mekanism.common.util.WorldUtils;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ItemNetworkReader extends ItemEnergized {

    public ItemNetworkReader(Properties properties) {
        super(MekanismConfig.gear.networkReaderChargeRate, MekanismConfig.gear.networkReaderMaxEnergy, properties.rarity(Rarity.UNCOMMON));
    }

    private void displayBorder(Player player, Object toDisplay, boolean brackets) {
        player.sendMessage(MekanismLang.NETWORK_READER_BORDER.translateColored(EnumColor.GRAY, "-------------", EnumColor.DARK_BLUE,
              brackets ? MekanismLang.GENERIC_SQUARE_BRACKET.translate(toDisplay) : toDisplay), Util.NIL_UUID);
    }

    private void displayEndBorder(Player player) {
        displayBorder(player, "[=======]", false);
    }

    @Nonnull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        if (!world.isClientSide && player != null) {
            BlockPos pos = context.getClickedPos();
            BlockEntity tile = WorldUtils.getTileEntity(world, pos);
            if (tile != null) {
                if (!player.isCreative()) {
                    FloatingLong energyPerUse = MekanismConfig.gear.networkReaderEnergyUsage.get();
                    IEnergyContainer energyContainer = StorageUtils.getEnergyContainer(context.getItemInHand(), 0);
                    if (energyContainer == null || energyContainer.extract(energyPerUse, Action.SIMULATE, AutomationType.MANUAL).smallerThan(energyPerUse)) {
                        return InteractionResult.FAIL;
                    }
                    energyContainer.extract(energyPerUse, Action.EXECUTE, AutomationType.MANUAL);
                }
                Direction opposite = context.getClickedFace().getOpposite();
                if (tile instanceof TileEntityTransmitter transmitterTile) {
                    displayTransmitterInfo(player, transmitterTile.getTransmitter(), tile, opposite);
                } else {
                    Optional<IHeatHandler> heatHandler = CapabilityUtils.getCapability(tile, Capabilities.HEAT_HANDLER_CAPABILITY, opposite).resolve();
                    if (heatHandler.isPresent()) {
                        IHeatHandler transfer = heatHandler.get();
                        displayBorder(player, MekanismLang.MEKANISM, true);
                        sendTemperature(player, transfer);
                        displayEndBorder(player);
                    } else {
                        displayConnectedNetworks(player, world, pos);
                    }
                }
                return InteractionResult.CONSUME;
            } else if (player.isShiftKeyDown() && MekanismAPI.debug) {
                displayBorder(player, MekanismLang.DEBUG_TITLE, true);
                for (Component component : TransmitterNetworkRegistry.getInstance().toComponents()) {
                    player.sendMessage(TextComponentUtil.build(EnumColor.DARK_GRAY, component), Util.NIL_UUID);
                }
                displayEndBorder(player);
            }
        }
        return InteractionResult.PASS;
    }

    private void displayTransmitterInfo(Player player, Transmitter<?, ?, ?> transmitter, BlockEntity tile, Direction opposite) {
        displayBorder(player, MekanismLang.MEKANISM, true);
        if (transmitter.hasTransmitterNetwork()) {
            DynamicNetwork<?, ?, ?> transmitterNetwork = transmitter.getTransmitterNetwork();
            player.sendMessage(MekanismLang.NETWORK_READER_TRANSMITTERS.translateColored(EnumColor.GRAY, EnumColor.DARK_GRAY, transmitterNetwork.transmittersSize()), Util.NIL_UUID);
            player.sendMessage(MekanismLang.NETWORK_READER_ACCEPTORS.translateColored(EnumColor.GRAY, EnumColor.DARK_GRAY, transmitterNetwork.getAcceptorCount()), Util.NIL_UUID);
            sendMessageIfNonNull(player, MekanismLang.NETWORK_READER_NEEDED, transmitterNetwork.getNeededInfo());
            sendMessageIfNonNull(player, MekanismLang.NETWORK_READER_BUFFER, transmitterNetwork.getStoredInfo());
            sendMessageIfNonNull(player, MekanismLang.NETWORK_READER_THROUGHPUT, transmitterNetwork.getFlowInfo());
            sendMessageIfNonNull(player, MekanismLang.NETWORK_READER_CAPACITY, transmitterNetwork.getNetworkReaderCapacity());
            CapabilityUtils.getCapability(tile, Capabilities.HEAT_HANDLER_CAPABILITY, opposite).ifPresent(heatHandler -> sendTemperature(player, heatHandler));
        } else {
            player.sendMessage(MekanismLang.NO_NETWORK.translate(), Util.NIL_UUID);
        }
        displayEndBorder(player);
    }

    private void displayConnectedNetworks(Player player, Level world, BlockPos pos) {
        Set<DynamicNetwork<?, ?, ?>> iteratedNetworks = new ObjectOpenHashSet<>();
        for (Direction side : EnumUtils.DIRECTIONS) {
            BlockEntity tile = WorldUtils.getTileEntity(world, pos.relative(side));
            if (tile instanceof TileEntityTransmitter transmitterTile) {
                Transmitter<?, ?, ?> transmitter = transmitterTile.getTransmitter();
                DynamicNetwork<?, ?, ?> transmitterNetwork = transmitter.getTransmitterNetwork();
                if (transmitterNetwork.hasAcceptor(pos) && !iteratedNetworks.contains(transmitterNetwork)) {
                    displayBorder(player, compileList(transmitter.getSupportedTransmissionTypes()), false);
                    player.sendMessage(MekanismLang.NETWORK_READER_CONNECTED_SIDES.translateColored(EnumColor.GRAY, EnumColor.DARK_GRAY,
                          compileList(transmitterNetwork.getAcceptorDirections(pos))), Util.NIL_UUID);
                    displayEndBorder(player);
                    iteratedNetworks.add(transmitterNetwork);
                }
            }
        }
    }

    private void sendTemperature(Player player, IHeatHandler handler) {
        Component temp = MekanismUtils.getTemperatureDisplay(handler.getTotalTemperature(), TemperatureUnit.KELVIN, true);
        player.sendMessage(MekanismLang.NETWORK_READER_TEMPERATURE.translateColored(EnumColor.GRAY, EnumColor.DARK_GRAY, temp), Util.NIL_UUID);
    }

    private void sendMessageIfNonNull(Player player, ILangEntry langEntry, Object toSend) {
        if (toSend != null) {
            player.sendMessage(langEntry.translateColored(EnumColor.GRAY, EnumColor.DARK_GRAY, toSend), Util.NIL_UUID);
        }
    }

    private <ENUM extends Enum<ENUM>> Component compileList(Set<ENUM> elements) {
        if (elements.isEmpty()) {
            return MekanismLang.GENERIC_SQUARE_BRACKET.translate("");
        }
        Component component = null;
        for (ENUM element : elements) {
            if (component == null) {
                component = TextComponentUtil.build(element);
            } else {
                component = MekanismLang.GENERIC_WITH_COMMA.translate(component, element);
            }
        }
        return MekanismLang.GENERIC_SQUARE_BRACKET.translate(component);
    }
}