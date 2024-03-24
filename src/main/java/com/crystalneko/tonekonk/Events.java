package com.crystalneko.tonekonk;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import com.crystalneko.tonekonk.api.NekoSet;


public class Events implements Listener {
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        String message = event.getMessage();
        if(NekoSet.isNeko(name)){
            message = "[§a猫娘§f§r]"+ name + "§6>> §f" + message;

        }
    }
}
