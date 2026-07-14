package org.cneko.toneko.bukkit.events;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.cneko.toneko.bukkit.ToNeko;
import org.cneko.toneko.common.api.NekoQuery;

public class PlayerConnectionEvents implements Listener {
    public static void init(){
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerConnectionEvents(), ToNeko.INSTANCE);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        if(neko.isNeko()){
            neko.fixQuirks();
            String nick = neko.getNickName().isEmpty() ? player.getName() : neko.getNickName();
            Bukkit.broadcast(Component.text("§d" + nick + " §ejoined the server §d❤"));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        NekoQuery.NekoData.saveAndRemoveNeko(player.getUniqueId());
    }
}
