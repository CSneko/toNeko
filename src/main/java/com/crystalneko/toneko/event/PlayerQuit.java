package com.crystalneko.toneko.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import com.crystalneko.ctlib.chat.chatPrefix;

public class PlayerQuit implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        //删除前缀
        chatPrefix.subPrivatePrefix(player,"§a猫娘");
    }
}
