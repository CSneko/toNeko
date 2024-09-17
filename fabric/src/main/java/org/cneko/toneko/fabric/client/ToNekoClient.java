package org.cneko.toneko.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import org.cneko.toneko.fabric.client.renderers.NekoRenderer;
import org.cneko.toneko.fabric.client.events.ClientNetworkEvents;
import org.cneko.toneko.fabric.client.events.ClientPlayerJoinEvent;
import org.cneko.toneko.fabric.client.events.ClientTickEvent;
import org.cneko.toneko.fabric.client.items.NekoArmorTrinketsRenderer;
import org.cneko.toneko.fabric.entities.ToNekoEntities;
import org.cneko.toneko.fabric.items.ToNekoItems;

public class ToNekoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ToNekoKeyBindings.init();
        ClientNetworkEvents.init();
        ClientTickEvent.init();
        ClientPlayerJoinEvent.init();
        // 注册trinkets渲染器
        if (ToNekoItems.isTrinketsInstalled && ToNekoItems.isGeckolibInstalled){
            Minecraft.getInstance().execute(NekoArmorTrinketsRenderer::init);
        }
        EntityRendererRegistry.register(ToNekoEntities.ADVENTURER_NEKO, NekoRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.CRYSTAL_NEKO, NekoRenderer::new);
    }
}
