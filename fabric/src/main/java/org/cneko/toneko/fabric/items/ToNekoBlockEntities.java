package org.cneko.toneko.fabric.items;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.cneko.toneko.common.mod.blocks.ClotheslineBlockEntity;

import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.blocks.ToNekoBlockEntities.*;

public class ToNekoBlockEntities {
    public static void init() {
        CLOTHESLINE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(MODID, "clothesline"),
                build(ClotheslineBlockEntity::new, org.cneko.toneko.common.mod.blocks.ToNekoBlocks.CLOTHESLINE));
    }
}
