package com.crystalneko.toneko.chat;

import com.crystalneko.toneko.ToNeko;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

import java.io.File;
import java.util.Random;

import static org.bukkit.Bukkit.getServer;

public class nekoed implements Listener{
    private ToNeko plugin;
    public nekoed(ToNeko plugin) {
        this.plugin = plugin;
        //注册玩家聊天监听器
        getServer().getPluginManager().registerEvents(this, plugin);

    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        //创建数据文件实例
        File dataFile = new File( "plugins/toNeko/nekos.yml");
        // 加载数据文件
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        Player player = event.getPlayer();
        String message = event.getMessage();
        //判断是否有主人
        if(data.getString(player.getDisplayName() + ".owner") != null) {
            //获取主人名称
            String  owner =data.getString(player.getDisplayName()+".owner");
            // 对消息进行处理
            String catMessage = catChatMessage(player, message,owner);

            // 修改消息的格式并重新发送
            event.setFormat("[§a猫娘§f]" + player.getDisplayName() + " >> " + catMessage);
        } else {
            event.setFormat(player.getDisplayName() + " >> " + message);
        }

    }
    public String catChatMessage(Player player,String message,String owner){
        //将玩家名称替换为主人
        message = message.replaceAll("\\b"+ owner +"\\b", "主人");
        //随机将",，"替换为"喵~"
        message = replaceChar(message, ',',"喵~",0.4);
        message = replaceChar(message, '，',"喵~",0.4);
        //将最后替换成"喵~"
        if(toString().endsWith("喵~")){}else {
            if (toString().endsWith("。")) {
                message = replaceChar(message,'。',"喵",1.0);
            }else {
                message = message + "喵~";
            }
        }
        return message;
    }

    public String replaceChar(String str, char oldChar, String newStr, double probability) {
        StringBuilder builder = new StringBuilder(str);
        Random random = new Random();

        for (int i = 0; i < builder.length(); i++) {
            if (builder.charAt(i) == oldChar && random.nextDouble() <= probability) {
                builder.replace(i, i + 1, newStr);
                i += newStr.length() - 1;
            }
        }

        return builder.toString();
    }

}
