package org.cneko.toneko.bukkit.events;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.cneko.toneko.bukkit.util.PlaceHolderUtil;
import org.cneko.toneko.common.Stats;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.api.json.NekoDataModel;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.api.Messaging;

import java.util.List;
import java.util.UUID;

import static org.cneko.toneko.bukkit.ToNeko.INSTANCE;
import static org.cneko.toneko.common.Bootstrap.LOGGER;
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
        message = Messaging.nekoModify(message, neko);
        // 格式化消息
        message = format(message, player, nickname);
        sendMessage(message);
        // 消息中喵的数量
        int count = Stats.getMeow(message);
        // 根据喵的数量增加经验
        neko.addLevel((double) count / 1000.00);
        if(ConfigUtil.isStatsEnable()) Stats.meowInChat(player.getName(),count);
    }

    private String format(String message, Player player, String nickname){
        String format = PlaceHolderUtil.replace(player,ConfigUtil.getChatFormat());
        return Messaging.format(message,player.getName(),nickname,format);
    }

    public static void sendMessage(String message){
        Bukkit.getServer().sendMessage(Component.text(message));
        // 输出到控制台（并清除格式化代码）
        LOGGER.info(message.replaceAll("§[0-9a-fk-or]",""));
    }

    /**
     * 通用的聊天消息处理，具体的聊天消息处理逻辑由各个端的监听器实现
     * @param message 聊天消息
     * @param neko 猫娘对象
     * @return 修改后的消息
     */
    public static String modify(String message, NekoQuery.Neko neko){
        if(neko.isNeko()){
            List<NekoDataModel.Owner> owners = neko.getOwners();
            // 替换主人名称
            for(NekoDataModel.Owner owner:owners){
                UUID uuid = owner.getUuid();
                Player player = Bukkit.getPlayer(uuid);
                if(player!=null){
                    // 替换主人名称
                    String name = player.getName();
                    String t = translatable("misc.toneko.owner");
                    message = message.replace(name,t);
                    // 替换别名
                    List<String> alias = owner.getAliases();
                    for (String s : alias) {
                        message = message.replace(s, t);
                    }
                }
            }
        }
        return message;
    }
}
