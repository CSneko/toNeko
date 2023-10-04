package com.crystalneko.toneko.chat;

import com.crystalneko.ctlib.chat.chatPrefix;
import com.crystalneko.toneko.ToNeko;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
        //获取前缀
        String publicPrefix = chatPrefix.getAllPublicPrefixValues();
        String privatePrefix = chatPrefix.getPrivatePrefix(player);
        //判断是否有私有前缀
        if(privatePrefix.equalsIgnoreCase("[§a无前缀§f§r]")){
            privatePrefix = "";
        } else if (privatePrefix.equalsIgnoreCase("[§a无任何前缀§f§r]")) {
            privatePrefix = "";
        }
        String prefix = publicPrefix + privatePrefix;
        //判断是否有主人
        if(data.getString(player.getName() + ".owner") != null) {
            //获取主人名称
            String owner =data.getString(player.getName()+".owner");
            List<String> aliases = new ArrayList<>();
            //获取主人别名
            if (data.getList(player.getName()+".aliases") !=null){
                aliases = data.getStringList(player.getName() + ".aliases");
            } else {
                //算是夹带私货吧(ps:这是我的正版账户名称)
                aliases.add("Crystal_Neko");
            }
            // 对消息进行处理
            String catMessage = catChatMessage(message,owner,aliases);
            // 修改消息的格式并重新发送
            event.setFormat(prefix + player.getName() + " >> §7" + catMessage);
        } else {
            event.setFormat(prefix + player.getName() + " >> §7" + message);
        }

    }
    public String catChatMessage(String message, String owner, List<String> aliases){
        //将玩家名称替换为主人
        message = message.replaceAll(owner, "主人");
        //将别名替换为主人
        for (String value : aliases) {
            message = message.replaceAll(value, "主人");
        }
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
