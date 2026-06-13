package org.cneko.toneko.neoforge.items;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.blocks.CatnipBlock;
import org.cneko.toneko.common.mod.blocks.NekoAggregatorBlock;
import org.cneko.toneko.neoforge.ToNekoNeoForge;

import static org.cneko.toneko.common.mod.blocks.ToNekoBlocks.*;

public class ToNekoBlocks {
    public static DeferredHolder<Block, CatnipBlock> CATNIP_HOLDER;
    public static DeferredHolder<Block, Block> NEKO_AGGREGATOR_BLOCK_HOLDER;
    public static DeferredHolder<Block, Block> NEKO_BLOCK_HOLDER;
    public static DeferredHolder<Block, Block> NEKO_DIAMOND_BLOCK_HOLDER;
    public static void init(){
        CATNIP_HOLDER = ToNekoNeoForge.BLOCKS.register("catnip", CatnipBlock::new);
        NEKO_AGGREGATOR_BLOCK_HOLDER = ToNekoNeoForge.BLOCKS.register("neko_aggregator", () -> new NekoAggregatorBlock(Block.Properties.of()));
        NEKO_BLOCK_HOLDER = ToNekoNeoForge.BLOCKS.register("neko_block", () -> new Block(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops()));
        NEKO_DIAMOND_BLOCK_HOLDER = ToNekoNeoForge.BLOCKS.register("neko_diamond_block", () -> new Block(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops()));
    }

    public static void reg(){
        CATNIP = CATNIP_HOLDER.get();
        NEKO_AGGREGATOR = NEKO_AGGREGATOR_BLOCK_HOLDER.get();
        NEKO_BLOCK = NEKO_BLOCK_HOLDER.get();
        NEKO_DIAMOND_BLOCK = NEKO_DIAMOND_BLOCK_HOLDER.get();
    }
}
