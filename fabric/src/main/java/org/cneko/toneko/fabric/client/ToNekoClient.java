package org.cneko.toneko.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import org.cneko.toneko.common.mod.client.ToNekoKeyBindings;
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer;
import org.cneko.toneko.common.mod.client.events.ClientNetworkEvents;
import org.cneko.toneko.common.mod.client.events.ClientPlayerJoinEvent;
import org.cneko.toneko.common.mod.client.events.ClientTickEvent;
import org.cneko.toneko.fabric.client.items.NekoArmorTrinketsRenderer;
import org.cneko.toneko.common.mod.entities.ToNekoEntities;
import org.cneko.toneko.fabric.items.ToNekoItems;

public class ToNekoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ToNekoKeyBindings.init();
        ClientNetworkEvents.init();
        ClientTickEvent.init();
        ClientPlayerJoinEvent.init();
        // 注册trinkets渲染器
        if (ToNekoItems.isTrinketsInstalled){
            Minecraft.getInstance().execute(NekoArmorTrinketsRenderer::init);
        }
        EntityRendererRegistry.register(ToNekoEntities.ADVENTURER_NEKO, NekoRenderer::new);
        EntityRendererRegistry.register(ToNekoEntities.CRYSTAL_NEKO, NekoRenderer::new);
    }
}
