package org.cneko.toneko.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.cneko.toneko.fabric.client.events.ClientNetworkPacketEvent;
import org.cneko.toneko.fabric.client.events.ClientTickEvent;
import org.cneko.toneko.fabric.network.packets.EntityPosePayload;

public class ToNekoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 客户端启动完成后
        ClientNetworkPacketEvent.init();
        ClientTickEvent.init();

        ClientPlayNetworking.registerGlobalReceiver(EntityPosePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ClientBlockHighlighting.highlightBlock(client, payload.blockPos());
            });
        });
    }
}
