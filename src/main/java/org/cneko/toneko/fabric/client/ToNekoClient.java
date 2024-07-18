package org.cneko.toneko.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.impl.client.rendering.ArmorRendererRegistryImpl;
import net.minecraft.registry.Registry;
import org.cneko.toneko.fabric.client.events.ClientPlayerJoinEvent;
import org.cneko.toneko.fabric.client.events.ClientNetworkPacketEvent;
import org.cneko.toneko.fabric.client.events.ClientTickEvent;
import org.cneko.toneko.fabric.client.items.NekoTailRenderer;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ToNekoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworkPacketEvent.init();
        ClientTickEvent.init();
        ClientPlayerJoinEvent.init();

    }
}
