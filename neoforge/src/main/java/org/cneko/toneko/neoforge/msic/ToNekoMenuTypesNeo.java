package org.cneko.toneko.neoforge.msic;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.cneko.toneko.common.mod.blocks.NekoAggregatorBlock;

import static org.cneko.toneko.common.mod.recipes.ToNekoMenuTypes.*;
import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class ToNekoMenuTypesNeo {

    public static void init(IEventBus bus){
        // 立即创建 MenuType 对象并赋值，确保 MenuScreens.register() 调用时非 null
        NEKO_AGGREGATOR = new MenuType<>(NekoAggregatorBlock.NekoAggregatorMenu::new, FeatureFlags.VANILLA_SET);
        // 延迟到 RegisterEvent 再注册到原版注册表
        bus.addListener(ToNekoMenuTypesNeo::onRegisterMenus);
    }

    public static void onRegisterMenus(RegisterEvent event) {
        event.register(Registries.MENU, helper -> {
            helper.register(toNekoLoc("neko_aggregator"), NEKO_AGGREGATOR);
        });
    }

    public static void reg(){
        // 已在 init 中赋值，无需额外操作
    }
}
