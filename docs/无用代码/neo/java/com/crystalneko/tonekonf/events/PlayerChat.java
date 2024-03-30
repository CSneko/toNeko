package com.crystalneko.tonekonf.events;

import com.crystalneko.tonekofabric.event.playerChat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
public class PlayerChat {
    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        playerChat.onChatMessage(event.getPlayer(), event.getMessage());
    }

}
