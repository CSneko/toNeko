package org.cneko.toneko.neoforge.msic;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cneko.toneko.common.mod.blocks.NekoAggregatorBlock;

import static org.cneko.toneko.common.mod.recipes.ToNekoMenuTypes.*;
import static org.cneko.toneko.neoforge.ToNekoNeoForge.MENU_TYPES;

public class ToNekoMenuTypesNeo {
    static DeferredHolder<MenuType<?>,MenuType<NekoAggregatorBlock.NekoAggregatorMenu>> NEKO_AGGREGATOR_HOLDER;
    public static void init(){
        NEKO_AGGREGATOR_HOLDER = MENU_TYPES.register("neko_aggregator",()->new MenuType<>(NekoAggregatorBlock.NekoAggregatorMenu::new, FeatureFlags.VANILLA_SET));
    }

    public static void reg(){
        NEKO_AGGREGATOR = NEKO_AGGREGATOR_HOLDER.get();
    }
}
