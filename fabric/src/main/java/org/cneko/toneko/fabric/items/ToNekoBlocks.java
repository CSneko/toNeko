package org.cneko.toneko.fabric.items;

import org.cneko.toneko.common.mod.util.NekoIds;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.cneko.toneko.common.mod.blocks.*;

import static org.cneko.toneko.common.mod.blocks.ToNekoBlocks.*;
import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoBlocks {
    public static void init(){
        CATNIP = Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "catnip"),
                new CatnipBlock());
        WILD_CATNIP = Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "wild_catnip"),
                new WildCatnipBlock());
        NEKO_AGGREGATOR = Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "neko_aggregator"),
                new NekoAggregatorBlock(NekoIds.blockProps("neko_aggregator").strength(5.0f).requiresCorrectToolForDrops()));
        NEKO_BLOCK = Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "neko_block"),
                new Block(NekoIds.blockProps("neko_block").strength(5.0f).requiresCorrectToolForDrops()));
        NEKO_DIAMOND_BLOCK = Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "neko_diamond_block"),
                new Block(NekoIds.blockProps("neko_diamond_block").strength(5.0f).requiresCorrectToolForDrops()));
        SHENG_DENG = Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "sheng_deng"),
                new ShengDengBlock(NekoIds.blockProps("sheng_deng").strength(3.0f).sound(SoundType.BAMBOO_WOOD).noOcclusion()));
        LEGWEAR_WORKBENCH = Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "legwear_workbench"),
                new LegwearWorkbenchBlock(NekoIds.blockProps("legwear_workbench").strength(2.5f).sound(SoundType.WOOD)));
        CLOTHESLINE = Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "clothesline"),
                new ClotheslineBlock(NekoIds.blockProps("clothesline").strength(2.0f).sound(SoundType.WOOD).noOcclusion()));
    }
}
