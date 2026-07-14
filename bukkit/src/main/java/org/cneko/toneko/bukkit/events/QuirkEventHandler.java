package org.cneko.toneko.bukkit.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.cneko.toneko.bukkit.ToNeko;
import org.cneko.toneko.common.api.NekoQuery;
public class QuirkEventHandler implements Listener {

    public static void init() {
        Bukkit.getServer().getPluginManager().registerEvents(new QuirkEventHandler(), ToNeko.INSTANCE);
        startTickLoop();
    }

    private static void startTickLoop() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(ToNeko.INSTANCE, t -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                NekoQuery.Neko n = NekoQuery.getNeko(p.getUniqueId());
                if (!n.isNeko()) continue;
                n.fixQuirks(); // Remove invalid quirks
            }
        }, 1L, 20L);
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        NekoQuery.NekoData.saveAll();
    }
}
