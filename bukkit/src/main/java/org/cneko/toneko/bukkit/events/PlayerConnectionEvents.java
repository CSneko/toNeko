package org.cneko.toneko.bukkit.events;

import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.cneko.toneko.bukkit.ToNeko;
import org.cneko.toneko.bukkit.api.ClientStatus;
import org.cneko.toneko.bukkit.api.NekoStatus;
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
            // 修复quirks
            neko.fixQuirks();
            String name = player.getName();
            NekoStatus.addPrefix(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        if(neko.isNeko()){
            String name = player.getName();
            NekoStatus.removePrefix(player);
        }
        // 保存猫娘数据
        NekoQuery.NekoData.saveAndRemoveNeko(player.getUniqueId());
    }
}
