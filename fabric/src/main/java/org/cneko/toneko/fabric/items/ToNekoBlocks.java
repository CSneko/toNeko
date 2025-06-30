package org.cneko.toneko.fabric.items;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.cneko.toneko.common.mod.blocks.*;

import static org.cneko.toneko.common.mod.blocks.ToNekoBlocks.*;
import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoBlocks {
    public static void init(){
        CATNIP = Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "catnip"),
                new CatnipBlock());
        NEKO_AGGREGATOR = Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "neko_aggregator"),
                new NekoAggregatorBlock(BlockBehaviour.Properties.of()));
    }
}
