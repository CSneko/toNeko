package org.cneko.toneko.bukkit.events;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.ctlib.common.util.ChatPrefix;
import org.cneko.toneko.bukkit.api.NekoStatus;
import org.cneko.toneko.bukkit.util.PlaceHolderUtil;
import org.cneko.toneko.common.Stats;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.common.util.Messaging;

import java.util.List;
import java.util.UUID;

import static org.cneko.toneko.bukkit.ToNeko.INSTANCE;
import static org.cneko.toneko.common.util.LanguageUtil.translatable;

public class ChatEvent implements Listener {
    public static void init(){
        Bukkit.getServer().getPluginManager().registerEvents(new ChatEvent(), INSTANCE);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        NekoQuery.Neko neko = NekoQuery.getNeko(player.getUniqueId());
        String message = event.signedMessage().message();
        // 获取昵称
        String nickname = neko.getNickName();
        message = modify(message, neko);
        // 获取前缀
        List<String> prefix = NekoStatus.getPlayerPrefixes(player);
        String p = formatPrefixes(prefix);
        // 格式化消息
        message = format(message, player, nickname, p);
        sendMessage(message);
        // 消息中喵的数量
        int count = Stats.getMeow(message);
        // 根据喵的数量增加经验
        neko.addLevel((double) count / 1000.00);
        if(ConfigUtil.STATS) Stats.meowInChat(player.getName(),count);
    }

    private String format(String message, Player player, String nickname,String prefix){
        String format = PlaceHolderUtil.replace(player,ConfigUtil.CHAT_FORMAT);
        return Messaging.format(message,player.getName(),nickname,prefix,format);
    }

    public static String formatPrefixes(List<String> prefixes) {
        StringBuilder formatted = new StringBuilder();

        for (String prefix : prefixes) {
            // 将每个前缀格式化为 [§a前缀§f§r]
            formatted.append("[§a").append(prefix).append("§f§r]");
        }

        return formatted.toString();
    }

    public static void sendMessage(String message){
        Bukkit.getServer().sendMessage(Component.text(message));
    }

    /**
     * 通用的聊天消息处理，具体的聊天消息处理逻辑由各个端的监听器实现
     * @param message 聊天消息
     * @param neko 猫娘对象
     * @return 修改后的消息
     */
    public static String modify(String message, NekoQuery.Neko neko){
        if(neko.isNeko()){
            message = nekoModify(message, neko);
        }
        return message;
    }
    public static String nekoModify(String message, NekoQuery.Neko neko){
        List<JsonConfiguration> owners= neko.getOwners();

        // 替换屏蔽词
        for (JsonConfiguration block : neko.getProfile().getJsonList("blockWords")){
            if(block.getString("method").equalsIgnoreCase("all")){
                // 如果屏蔽词的类型为all，则直接替换为屏蔽词
                message = block.getString("replace");
                break;
            }
            message = message.replace(block.getString("block"),block.getString("replace"));
        }

        // 替换主人名称
        for(JsonConfiguration owner:owners){
            String uuid = owner.getString("uuid");
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            if(player!=null){
                // 替换主人名称
                String name = player.getName();
                String t = translatable("misc.toneko.owner");
                message = message.replace(name,t);
                // 替换别名
                List<String> alias = owner.getStringList("alias");
                for (String s : alias) {
                    message = message.replace(s, t);
                }
            }
        }

        //添加口癖
        String phrase = LanguageUtil.phrase;
        phrase = translatable(phrase);
        message = Messaging.replacePhrase(message,phrase);
        return message;
    }
}
