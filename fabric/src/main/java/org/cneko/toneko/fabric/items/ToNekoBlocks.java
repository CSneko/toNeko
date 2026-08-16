package org.cneko.toneko.fabric.items;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.cneko.toneko.common.mod.blocks.*;

import static org.cneko.toneko.common.mod.blocks.ToNekoBlocks.*;
import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoBlocks {
    public static void init(){
        CATNIP = Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "catnip"),
                new CatnipBlock());
        WILD_CATNIP = Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "wild_catnip"),
                new WildCatnipBlock());
        NEKO_AGGREGATOR = Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "neko_aggregator"),
                new NekoAggregatorBlock(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops()));
        NEKO_BLOCK = Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "neko_block"),
                new Block(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops()));
        NEKO_DIAMOND_BLOCK = Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "neko_diamond_block"),
                new Block(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops()));
        SHENG_DENG = Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "sheng_deng"),
                new ShengDengBlock(BlockBehaviour.Properties.of().strength(3.0f).sound(SoundType.BAMBOO_WOOD).noOcclusion()));
        LEGWEAR_WORKBENCH = Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "legwear_workbench"),
                new LegwearWorkbenchBlock(BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.WOOD)));
        CLOTHESLINE = Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "clothesline"),
                new ClotheslineBlock(BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.WOOD).noOcclusion()));
    }
}
