package org.cneko.toneko.common.mod.client;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.cneko.gal.common.client.GalClient;
import org.cneko.toneko.common.mod.client.screens.LegwearWorkbenchScreen;
import org.cneko.toneko.common.mod.client.screens.NekoAggregatorScreen;
import org.cneko.toneko.common.mod.client.api.StompAnimations;
import org.cneko.toneko.common.mod.client.screens.NekoScreenRegistry;
import org.cneko.toneko.common.mod.recipes.ToNekoMenuTypes;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoClient {
    public static void init(){
        // 启动Gal
        GalClient.init();
        // 注册 PlayerAnimator 踩踏动画层
        StompAnimations.init();
        NekoScreenRegistry.init();
        FabricLoader.getInstance().getModContainer(MODID).ifPresent(container-> ResourceManagerHelper.registerBuiltinResourcePack(ResourceLocation.fromNamespaceAndPath(MODID, "moe"),container, Component.translatable("resourcePack.toneko.moe"), ResourcePackActivationType.NORMAL));
        MenuScreens.register(ToNekoMenuTypes.NEKO_AGGREGATOR, NekoAggregatorScreen::new);
        MenuScreens.register(ToNekoMenuTypes.LEGWEAR_WORKBENCH, LegwearWorkbenchScreen::new);
    }
}
