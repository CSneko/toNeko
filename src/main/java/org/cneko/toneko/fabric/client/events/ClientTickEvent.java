package org.cneko.toneko.fabric.client.events;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import static org.cneko.toneko.fabric.client.events.ClientNetworkPacketEvent.poses;
public class ClientTickEvent {
    public static void init(){
        ClientTickEvents.START_CLIENT_TICK.register(ClientTickEvent::onTick);
    }

    public static void onTick(MinecraftClient client) {
        if (client.player != null) {
            PlayerEntity player = client.player;
            if (poses.containsKey(player)) {
                player.setPose(poses.get(player));
            }
        }
    }
}
