package org.cneko.toneko.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.cneko.toneko.fabric.client.events.ClientPlayerJoinEvent;
import org.cneko.toneko.fabric.client.events.ClientNetworkPacketEvent;
import org.cneko.toneko.fabric.client.events.ClientTickEvent;
import org.cneko.toneko.fabric.client.items.NekoArmorTrinketsRenderer;
import org.cneko.toneko.fabric.items.ToNekoItems;

public class ToNekoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworkPacketEvent.init();
        ClientTickEvent.init();
        ClientPlayerJoinEvent.init();
        // 注册trinkets渲染器
        if (ToNekoItems.isTrinketsInstalled && ToNekoItems.isGeckolibInstalled){
            MinecraftClient.getInstance().execute(NekoArmorTrinketsRenderer::init);
        }
    }
}
