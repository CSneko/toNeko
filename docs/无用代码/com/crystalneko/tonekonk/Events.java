package com.crystalneko.tonekonk;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import com.crystalneko.tonekocommon.Stats;
import com.crystalneko.tonekonk.api.NekoSet;
import cn.nukkit.Player;
import cn.nukkit.Server;

import java.util.Map;
import java.util.UUID;


public class Events implements Listener {
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        String message = event.getMessage();
        if(NekoSet.isNeko(name)){
            message = message + "喵~";
            message = "[§a猫娘§f§r]"+ name + "§6>> §f" + message;
        }else {
            message = name + "§6>> §f" + message;
        }
        event.setCancelled(true);

        // 获取当前服务器实例
        Server server = Server.getInstance();
        // 获取当前所有在线玩家数组
        Map<UUID, Player> onlinePlayers = server.getOnlinePlayers();
        // 遍历在线玩家数组并进行相应操作
        for (Player p : onlinePlayers.values()) {
            p.sendMessage(message);
        }
        // 发送统计信息
        Stats.meowInChat(player.getName(), message);
    }
}
