package org.cneko.toneko.neoforge.items;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.blocks.CatnipBlock;
import org.cneko.toneko.common.mod.blocks.ClotheslineBlock;
import org.cneko.toneko.common.mod.blocks.LegwearWorkbenchBlock;
import org.cneko.toneko.common.mod.blocks.NekoAggregatorBlock;
import org.cneko.toneko.common.mod.blocks.ShengDengBlock;
import org.cneko.toneko.common.mod.blocks.WildCatnipBlock;
import org.cneko.toneko.neoforge.ToNekoNeoForge;

import static org.cneko.toneko.common.mod.blocks.ToNekoBlocks.*;

public class ToNekoBlocks {
    public static DeferredHolder<Block, CatnipBlock> CATNIP_HOLDER;
    public static DeferredHolder<Block, WildCatnipBlock> WILD_CATNIP_HOLDER;
    public static DeferredHolder<Block, Block> NEKO_AGGREGATOR_BLOCK_HOLDER;
    public static DeferredHolder<Block, Block> NEKO_BLOCK_HOLDER;
    public static DeferredHolder<Block, Block> NEKO_DIAMOND_BLOCK_HOLDER;
    public static DeferredHolder<Block, ShengDengBlock> SHENG_DENG_HOLDER;
    public static DeferredHolder<Block, LegwearWorkbenchBlock> LEGWEAR_WORKBENCH_HOLDER;
    public static DeferredHolder<Block, ClotheslineBlock> CLOTHESLINE_HOLDER;
    public static void init(){
        CATNIP_HOLDER = ToNekoNeoForge.BLOCKS.register("catnip", CatnipBlock::new);
        WILD_CATNIP_HOLDER = ToNekoNeoForge.BLOCKS.register("wild_catnip", WildCatnipBlock::new);
        NEKO_AGGREGATOR_BLOCK_HOLDER = ToNekoNeoForge.BLOCKS.register("neko_aggregator", () -> new NekoAggregatorBlock(Block.Properties.of()));
        NEKO_BLOCK_HOLDER = ToNekoNeoForge.BLOCKS.register("neko_block", () -> new Block(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops()));
        NEKO_DIAMOND_BLOCK_HOLDER = ToNekoNeoForge.BLOCKS.register("neko_diamond_block", () -> new Block(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops()));
        SHENG_DENG_HOLDER = ToNekoNeoForge.BLOCKS.register("sheng_deng", () -> new ShengDengBlock(BlockBehaviour.Properties.of().strength(3.0f).sound(net.minecraft.world.level.block.SoundType.BAMBOO_WOOD).noOcclusion()));
        LEGWEAR_WORKBENCH_HOLDER = ToNekoNeoForge.BLOCKS.register("legwear_workbench", () -> new LegwearWorkbenchBlock(BlockBehaviour.Properties.of().strength(2.5f).sound(net.minecraft.world.level.block.SoundType.WOOD)));
        CLOTHESLINE_HOLDER = ToNekoNeoForge.BLOCKS.register("clothesline", () -> new ClotheslineBlock(BlockBehaviour.Properties.of().strength(2.0f).sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion()));
    }

    public static void reg(){
        CATNIP = CATNIP_HOLDER.get();
        WILD_CATNIP = WILD_CATNIP_HOLDER.get();
        NEKO_AGGREGATOR = NEKO_AGGREGATOR_BLOCK_HOLDER.get();
        NEKO_BLOCK = NEKO_BLOCK_HOLDER.get();
        NEKO_DIAMOND_BLOCK = NEKO_DIAMOND_BLOCK_HOLDER.get();
        SHENG_DENG = SHENG_DENG_HOLDER.get();
        LEGWEAR_WORKBENCH = LEGWEAR_WORKBENCH_HOLDER.get();
        CLOTHESLINE = CLOTHESLINE_HOLDER.get();
    }
}
