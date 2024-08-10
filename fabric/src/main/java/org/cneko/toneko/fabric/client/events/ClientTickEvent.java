package org.cneko.toneko.fabric.client.events;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import static org.cneko.toneko.fabric.client.events.ClientNetworkPacketEvent.poses;
public class ClientTickEvent {
    public static void init(){
        ClientTickEvents.START_CLIENT_TICK.register(ClientTickEvent::onTick);
    }

    public static void onTick(Minecraft client) {
        if (client.player != null) {
            Player player = client.player;
            if (poses.containsKey(player)) {
                player.setPose(poses.get(player));
            }
        }
    }
}
