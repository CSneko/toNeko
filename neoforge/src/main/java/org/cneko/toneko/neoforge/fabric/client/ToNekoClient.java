package org.cneko.toneko.neoforge.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import org.cneko.toneko.neoforge.fabric.client.events.ClientNetworkPacketEvent;
import org.cneko.toneko.neoforge.fabric.client.events.ClientPlayerJoinEvent;
import org.cneko.toneko.neoforge.fabric.client.events.ClientTickEvent;

public class ToNekoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworkPacketEvent.init();
        ClientTickEvent.init();
        ClientPlayerJoinEvent.init();
    }
}
