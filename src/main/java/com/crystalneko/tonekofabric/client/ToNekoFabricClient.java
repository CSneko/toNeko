package com.crystalneko.tonekofabric.client;

import com.crystalneko.tonekofabric.ToNekoFabric;
import com.crystalneko.tonekofabric.entity.client.nekoRender;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ToNekoFabricClient implements ClientModInitializer {


    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ToNekoFabric.NEKO,nekoRender::new);
    }


}
