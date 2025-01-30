package org.cneko.toneko.bukkit.events;

import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.cneko.toneko.bukkit.ToNeko;
import org.cneko.toneko.common.api.NekoQuery;

import static org.bukkit.Bukkit.getServer;

public class WorldEvents implements Listener {
    public static void init(){
        getServer().getPluginManager().registerEvents(new WorldEvents(), ToNeko.INSTANCE);
    }

    public void onWorldLoad(WorldUnloadEvent event){
        NekoQuery.NekoData.saveAll();
    }
}
