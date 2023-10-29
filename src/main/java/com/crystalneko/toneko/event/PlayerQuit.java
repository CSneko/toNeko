package com.crystalneko.toneko.event;

import com.crystalneko.toneko.ToNeko;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import com.crystalneko.ctlib.chat.chatPrefix;

import java.io.File;

import static org.bukkit.Bukkit.getServer;

public class PlayerQuit implements Listener {
    private ToNeko plugin;
    public PlayerQuit(ToNeko plugin){
        this.plugin = plugin;
        getServer().getPluginManager().registerEvents(this,plugin );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        //创建数据文件实例
        File dataFile = new File( "plugins/toNeko/nekos.yml");
        // 加载数据文件
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        Player player = event.getPlayer();
        if(data.getString(player.getName() + ".owner") != null){
        //删除前缀
            chatPrefix.subPrivatePrefix(player, ToNeko.getMessage("other.neko"));
        }
    }
}
