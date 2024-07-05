package org.cneko.toneko.fabric.client.event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import static org.cneko.toneko.fabric.client.event.ClientNetworkPacketEvent.poses;
public class ClientTickEvent {
    public static void init(){
        ServerTickEvents.START_SERVER_TICK.register(ClientTickEvent::onTick);
    }

    public static void onTick(MinecraftServer server) {
        for (var player : server.getPlayerManager().getPlayerList()) {
            if(poses.containsKey(player)){
                player.setPose(poses.get(player));
            }
        }
    }
}
