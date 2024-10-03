package org.cneko.toneko.bukkit.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.cneko.ctlib.common.util.ChatPrefix;
import org.cneko.toneko.bukkit.ToNeko;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.util.LanguageUtil;

public class PlayerConnectionEvents implements Listener {
    public static void init(){
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerConnectionEvents(), ToNeko.INSTANCE);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        if(neko.isNeko()){
            // 修复quirks
            neko.fixQuirks();
            String name = player.getName();
            ChatPrefix.addPrivatePrefix(name, LanguageUtil.prefix);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        if(neko.isNeko()){
            String name = player.getName();
            ChatPrefix.removePrivatePrefix(name, LanguageUtil.prefix);
        }
        // 保存猫娘数据
        neko.save();
        NekoQuery.NekoData.removeNeko(player.getUniqueId());
    }
}
