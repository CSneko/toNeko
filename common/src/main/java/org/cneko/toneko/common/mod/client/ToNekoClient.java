package org.cneko.toneko.common.mod.client;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.cneko.gal.common.client.GalClient;
import org.cneko.toneko.common.mod.client.screens.NekoScreenRegistry;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNekoClient {
    public static void init(){
        // 启动Gal
        GalClient.init();
        NekoScreenRegistry.init();
        FabricLoader.getInstance().getModContainer(MODID).ifPresent(container-> ResourceManagerHelper.registerBuiltinResourcePack(ResourceLocation.fromNamespaceAndPath(MODID, "moe"),container, Component.translatable("resourcePack.toneko.moe"), ResourcePackActivationType.NORMAL));
    }
}
