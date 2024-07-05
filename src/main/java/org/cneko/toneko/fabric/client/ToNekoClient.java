package org.cneko.toneko.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import org.cneko.toneko.fabric.client.event.ClientNetworkPacketEvent;
import org.cneko.toneko.fabric.client.event.ClientTickEvent;

public class ToNekoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 客户端启动完成后
        ClientNetworkPacketEvent.init();
        ClientTickEvent.init();
    }
}
