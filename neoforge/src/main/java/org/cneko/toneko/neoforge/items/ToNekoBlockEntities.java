package org.cneko.toneko.neoforge.items;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.blocks.ClotheslineBlockEntity;
import org.cneko.toneko.neoforge.ToNekoNeoForge;

import static org.cneko.toneko.common.mod.blocks.ToNekoBlockEntities.*;

public class ToNekoBlockEntities {
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<ClotheslineBlockEntity>> CLOTHESLINE_HOLDER;

    public static void init() {
        CLOTHESLINE_HOLDER = ToNekoNeoForge.BLOCK_ENTITY_TYPES.register("clothesline",
                () -> build(ClotheslineBlockEntity::new, ToNekoBlocks.CLOTHESLINE_HOLDER.get()));
    }

    public static void reg() {
        CLOTHESLINE = CLOTHESLINE_HOLDER.get();
    }
}
