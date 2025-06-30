package org.cneko.toneko.fabric.msic;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.cneko.toneko.common.mod.blocks.NekoAggregatorBlock;

import static org.cneko.toneko.common.mod.recipes.ToNekoMenuTypes.*;
public class ToNekoMenuTypes {
    public static void init(){
        NEKO_AGGREGATOR = register("neko_aggregator", NekoAggregatorBlock.NekoAggregatorMenu::new);
    }

    private static <T extends AbstractContainerMenu> MenuType<T> register(String key, MenuType.MenuSupplier<T> factory) {
        return Registry.register(BuiltInRegistries.MENU, key, new MenuType<>(factory, FeatureFlags.VANILLA_SET));
    }
}
