package org.cneko.toneko.neoforge.items;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.blocks.CatnipBlock;
import org.cneko.toneko.neoforge.ToNekoNeoForge;
import static org.cneko.toneko.common.mod.blocks.ToNekoBlocks.*;

public class ToNekoBlocks {
    public static DeferredHolder<Block, CatnipBlock> CATNIP_HOLDER;
    public static void init(){
        CATNIP_HOLDER = ToNekoNeoForge.BLOCKS.register("catnip", CatnipBlock::new);
    }

    public static void reg(){
        CATNIP = CATNIP_HOLDER.get();
    }
}
