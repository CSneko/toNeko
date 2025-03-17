package org.cneko.toneko.fabric.items;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.cneko.toneko.common.mod.blocks.CatnipBlock;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoBlocks {
    public static void init(){
        org.cneko.toneko.common.mod.blocks.ToNekoBlocks.CATNIP = Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(MODID, "catnip"),
                new CatnipBlock());
    }
}
