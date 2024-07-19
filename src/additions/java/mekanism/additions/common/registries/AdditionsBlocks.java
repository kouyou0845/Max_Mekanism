package mekanism.additions.common.registries;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import mekanism.additions.common.MekanismAdditions;
import mekanism.additions.common.block.BlockGlowPanel;
import mekanism.additions.common.block.BlockObsidianTNT;
import mekanism.additions.common.block.plastic.BlockPlastic;
import mekanism.additions.common.block.plastic.BlockPlasticFence;
import mekanism.additions.common.block.plastic.BlockPlasticFenceGate;
import mekanism.additions.common.block.plastic.BlockPlasticRoad;
import mekanism.additions.common.block.plastic.BlockPlasticSlab;
import mekanism.additions.common.block.plastic.BlockPlasticStairs;
import mekanism.additions.common.block.plastic.BlockPlasticTransparent;
import mekanism.additions.common.block.plastic.BlockPlasticTransparentSlab;
import mekanism.additions.common.block.plastic.BlockPlasticTransparentStairs;
import mekanism.api.providers.IBlockProvider;
import mekanism.api.text.EnumColor;
import mekanism.common.block.interfaces.IColoredBlock;
import mekanism.common.item.block.ItemBlockColoredName;
import mekanism.common.registration.impl.BlockDeferredRegister;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.util.EnumUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class AdditionsBlocks {

    private AdditionsBlocks() {
    }

    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(MekanismAdditions.MODID);

    public static final BlockRegistryObject<BlockObsidianTNT, BlockItem> OBSIDIAN_TNT = BLOCKS.register("obsidian_tnt", BlockObsidianTNT::new);

    public static final Map<EnumColor, BlockRegistryObject<BlockGlowPanel, ItemBlockColoredName>> GLOW_PANELS = new EnumMap<>(EnumColor.class);
    public static final Map<EnumColor, BlockRegistryObject<BlockPlastic, ItemBlockColoredName>> PLASTIC_BLOCKS = new EnumMap<>(EnumColor.class);
    public static final Map<EnumColor, BlockRegistryObject<BlockPlastic, ItemBlockColoredName>> SLICK_PLASTIC_BLOCKS = new EnumMap<>(EnumColor.class);
    public static final Map<EnumColor, BlockRegistryObject<BlockPlastic, ItemBlockColoredName>> PLASTIC_GLOW_BLOCKS = new EnumMap<>(EnumColor.class);
    public static final Map<EnumColor, BlockRegistryObject<BlockPlastic, ItemBlockColoredName>> REINFORCED_PLASTIC_BLOCKS = new EnumMap<>(EnumColor.class);
    public static final Map<EnumColor, BlockRegistryObject<BlockPlasticRoad, ItemBlockColoredName>> PLASTIC_ROADS = new EnumMap<>(EnumColor.class);
    public static final Map<EnumColor, BlockRegistryObject<BlockPlasticTransparent, ItemBlockColoredName>> TRANSPARENT_PLASTIC_BLOCKS = new EnumMap<>(EnumColor.class);
    public static final Map<EnumColor, BlockRegistryObject<BlockPlasticStairs, ItemBlockColoredName>> PLASTIC_STAIRS = new EnumMap<>(EnumColor.class);
    public static final Map<EnumColor, BlockRegistryObject<BlockPlasticSlab, ItemBlockColoredName>> PLASTIC_SLABS = new EnumMap<>(EnumColor.class);
    public static final Map<EnumColor, BlockRegistryObject<BlockPlasticFence, ItemBlockColoredName>> PLASTIC_FENCES = new EnumMap<>(EnumColor.class);
    public static final Map<EnumColor, BlockRegistryObject<BlockPlasticFenceGate, ItemBlockColoredName>> PLASTIC_FENCE_GATES = new EnumMap<>(EnumColor.class);
    public static final Map<EnumColor, BlockRegistryObject<BlockPlasticStairs, ItemBlockColoredName>> PLASTIC_GLOW_STAIRS = new EnumMap<>(EnumColor.class);
    public static final Map<EnumColor, BlockRegistryObject<BlockPlasticSlab, ItemBlockColoredName>> PLASTIC_GLOW_SLABS = new EnumMap<>(EnumColor.class);
    public static final Map<EnumColor, BlockRegistryObject<BlockPlasticTransparentStairs, ItemBlockColoredName>> TRANSPARENT_PLASTIC_STAIRS = new EnumMap<>(EnumColor.class);
    public static final Map<EnumColor, BlockRegistryObject<BlockPlasticTransparentSlab, ItemBlockColoredName>> TRANSPARENT_PLASTIC_SLABS = new EnumMap<>(EnumColor.class);

    static {
        for (EnumColor color : EnumUtils.COLORS) {
            GLOW_PANELS.put(color, registerColoredBlock(BlockGlowPanel::new, "_glow_panel", color));
            BlockRegistryObject<BlockPlastic, ItemBlockColoredName> plasticBlockRO = registerPlastic(color, "_plastic",
                  properties -> properties.strength(5, 6));
            PLASTIC_BLOCKS.put(color, plasticBlockRO);
            SLICK_PLASTIC_BLOCKS.put(color, registerPlastic(color, "_slick_plastic", properties -> properties.strength(5, 6).friction(0.98F)));
            BlockRegistryObject<BlockPlastic, ItemBlockColoredName> plasticGlowBlockRO = registerPlastic(color, "_plastic_glow",
                  properties -> properties.strength(5, 6).lightLevel(state -> 10));
            PLASTIC_GLOW_BLOCKS.put(color, plasticGlowBlockRO);
            REINFORCED_PLASTIC_BLOCKS.put(color, registerPlastic(color, "_reinforced_plastic", properties -> properties.strength(50, 1_200)));
            PLASTIC_ROADS.put(color, registerColoredBlock(BlockPlasticRoad::new, "_plastic_road", color));
            BlockRegistryObject<BlockPlasticTransparent, ItemBlockColoredName> transparentPlasticRO = registerColoredBlock(BlockPlasticTransparent::new,
                  "_plastic_transparent", color);
            TRANSPARENT_PLASTIC_BLOCKS.put(color, transparentPlasticRO);
            PLASTIC_STAIRS.put(color, registerPlasticStairs(plasticBlockRO, color, "_plastic_stairs", UnaryOperator.identity()));
            PLASTIC_SLABS.put(color, registerPlasticSlab(color, "_plastic_slab", UnaryOperator.identity()));
            PLASTIC_FENCES.put(color, registerColoredBlock(BlockPlasticFence::new, "_plastic_fence", color));
            PLASTIC_FENCE_GATES.put(color, registerColoredBlock(BlockPlasticFenceGate::new, "_plastic_fence_gate", color));
            PLASTIC_GLOW_STAIRS.put(color, registerPlasticStairs(plasticGlowBlockRO, color, "_plastic_glow_stairs", properties -> properties.lightLevel(state -> 10)));
            PLASTIC_GLOW_SLABS.put(color, registerPlasticSlab(color, "_plastic_glow_slab", properties -> properties.lightLevel(state -> 10)));
            TRANSPARENT_PLASTIC_STAIRS.put(color, registerColoredBlock(c -> new BlockPlasticTransparentStairs(transparentPlasticRO, c),
                  "_plastic_transparent_stairs", color));
            TRANSPARENT_PLASTIC_SLABS.put(color, registerColoredBlock(BlockPlasticTransparentSlab::new, "_plastic_transparent_slab", color));
        }
    }

    private static BlockRegistryObject<BlockPlastic, ItemBlockColoredName> registerPlastic(EnumColor color, String blockTypeSuffix,
          UnaryOperator<BlockBehaviour.Properties> propertyModifier) {
        return registerColoredBlock(c -> new BlockPlastic(c, propertyModifier), blockTypeSuffix, color);
    }

    private static BlockRegistryObject<BlockPlasticSlab, ItemBlockColoredName> registerPlasticSlab(EnumColor color, String blockTypeSuffix,
          UnaryOperator<BlockBehaviour.Properties> propertyModifier) {
        return registerColoredBlock(c -> new BlockPlasticSlab(c, propertyModifier), blockTypeSuffix, color);
    }

    private static BlockRegistryObject<BlockPlasticStairs, ItemBlockColoredName> registerPlasticStairs(IBlockProvider baseBlock, EnumColor color, String blockTypeSuffix,
          UnaryOperator<BlockBehaviour.Properties> propertyModifier) {
        return registerColoredBlock(c -> new BlockPlasticStairs(baseBlock, c, propertyModifier), blockTypeSuffix, color);
    }

    private static <BLOCK extends Block & IColoredBlock> BlockRegistryObject<BLOCK, ItemBlockColoredName> registerColoredBlock(Function<EnumColor, BLOCK> blockCreator,
          String blockTypeSuffix, EnumColor color) {
        return BLOCKS.register(color.getRegistryPrefix() + blockTypeSuffix, () -> blockCreator.apply(color), ItemBlockColoredName::new);
    }
}