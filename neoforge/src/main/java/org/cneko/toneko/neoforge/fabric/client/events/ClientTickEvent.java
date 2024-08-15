package org.cneko.toneko.neoforge.fabric.client.events;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.neoforge.fabric.client.ToNekoKeyBindings;

import static org.cneko.toneko.neoforge.fabric.client.events.ClientNetworkPacketEvent.poses;
@Environment(EnvType.CLIENT)
public class ClientTickEvent {
    public static void init(){
        ClientTickEvents.START_CLIENT_TICK.register(ClientTickEvent::onTick);
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvent::processKeyInput);
    }

    public static void processKeyInput(Minecraft client) {
        while (ToNekoKeyBindings.LIE_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko lie");
        }
        while (ToNekoKeyBindings.GET_DOWN_KEY.consumeClick()) {
            client.player.connection.sendUnsignedCommand("neko getDown");
        }
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
