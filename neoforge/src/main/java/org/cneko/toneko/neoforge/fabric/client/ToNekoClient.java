package org.cneko.toneko.neoforge.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import org.cneko.toneko.neoforge.fabric.client.events.ClientNetworkPacketEvent;
import org.cneko.toneko.neoforge.fabric.client.events.ClientPlayerJoinEvent;
import org.cneko.toneko.neoforge.fabric.client.events.ClientTickEvent;

public class ToNekoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ToNekoKeyBindings.init();
        ClientNetworkPacketEvent.init();
        ClientTickEvent.init();
        ClientPlayerJoinEvent.init();
        //EntityRendererRegistry.register(ToNekoEntities.ADVENTURER_NEKO, NekoRenderer::new);
    }
}
