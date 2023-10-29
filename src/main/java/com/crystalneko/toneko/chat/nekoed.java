package com.crystalneko.toneko.chat;

import com.crystalneko.ctlib.chat.chatPrefix;
import com.crystalneko.ctlib.sql.sqlite;
import com.crystalneko.toneko.ToNeko;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
            //替换屏蔽词
            catMessage = replaceBlocks(catMessage,player.getName());
            // 修改消息的格式并重新发送
            event.setFormat(prefix + player.getName() + " >> §7" + catMessage);
        } else {
            event.setFormat(prefix + player.getName() + " >> §7" + message);
        }

    }
    public String catChatMessage(String message, String owner, List<String> aliases){
        //将玩家名称替换为主人
        message = message.replaceAll(owner, ToNeko.getMessage("other.owner"));
        //将别名替换为主人
        for (String value : aliases) {
            message = message.replaceAll(value, ToNeko.getMessage("other.owner"));
        }
        //随机将",，"替换为"喵~"
        message = replaceChar(message, ',',ToNeko.getMessage("other.nya"),0.4);
        message = replaceChar(message, '，',ToNeko.getMessage("other.nya"),0.4);
        //将最后替换成"喵~"
        if(!toString().endsWith(ToNeko.getMessage("other.nya"))){
            if (toString().endsWith(".")) {
                message = replaceChar(message,'.',ToNeko.getMessage("other.nya"),1.0);
            }else {
                message = message + ToNeko.getMessage("other.nya");
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
    private String replaceBlocks(String message,String neko){
        //检查值是否存在
        if(sqlite.checkValueExists("nekoblockword", "neko", neko)) {
            //读取数据
            String block = sqlite.getColumnValue("nekoblockword", "block", "neko", neko);
            String replace = sqlite.getColumnValue("nekoblockword", "replace", "neko", neko);
            String method = sqlite.getColumnValue("nekoblockword", "method", "neko", neko);
            if(block != null) {
                //转换为数组
                String[] blocks = block.split(",");
                String[] replaces = replace.split(",");
                String[] methods = method.split(",");
                //获取数组长度
                int length = blocks.length;
                //判断是否存在all
                int allIndex = Arrays.binarySearch(methods, "all");
                //判断是否存在屏蔽词
                if(allIndex >= 0 && message.contains(blocks[allIndex])){
                    //直接替换屏蔽词
                    message = message.replaceAll(message,replaces[allIndex]);
                } else {
                    //循环替换
                    int i = 0;
                    while (i < length) {
                        message = message.replaceAll(blocks[i], replaces[i]);
                        i ++;
                    }
                }
            }
        }
        return message;
    }
}
