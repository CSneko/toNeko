package com.crystalneko.tonekofabric.client;

import com.crystalneko.tonekofabric.ToNekoFabric;
import com.crystalneko.tonekofabric.client.screens.InstalledOptifine;
import com.crystalneko.tonekofabric.entity.client.nekoRender;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

public class ToNekoFabricClient implements ClientModInitializer {


    @Override
    public void onInitializeClient() {
        // 注册实体渲染器
        EntityRendererRegistry.register(ToNekoFabric.NEKO, nekoRender::new);

        // 当客户端启动完成时执行
        MinecraftClient.getInstance().execute(() -> {
            if(FabricLoader.getInstance().isModLoaded("optifabric")){
                // 如果安装了optifine
                MinecraftClient.getInstance().setScreen(new InstalledOptifine());
            }
        });

    }


}
