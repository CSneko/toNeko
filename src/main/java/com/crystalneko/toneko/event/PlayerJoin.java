package com.crystalneko.toneko.event;

import com.crystalneko.ctlib.chat.chatPrefix;
import com.crystalneko.toneko.ToNeko;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;

import static org.bukkit.Bukkit.getServer;

public class PlayerJoin implements Listener {
    private ToNeko plugin;
    public PlayerJoin(ToNeko plugin){
        this.plugin = plugin;
        getServer().getPluginManager().registerEvents(this,plugin );
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        //创建数据文件实例
        File dataFile = new File( "plugins/toNeko/nekos.yml");
        // 加载数据文件
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        Player player = event.getPlayer();
        //判断是否有主人
        if(data.getString(player.getDisplayName() + ".owner") != null) {
        //添加前缀
            chatPrefix.addPrivatePrefix(player,"§a猫娘");
        } else {}
    }

}