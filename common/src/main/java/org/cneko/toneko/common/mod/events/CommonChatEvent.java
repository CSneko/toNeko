package org.cneko.toneko.common.mod.events;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.toneko.common.Stats;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.util.PlayerUtil;
import org.cneko.toneko.common.mod.util.TextUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.common.util.Messaging;

import java.util.List;
import java.util.UUID;

import static org.cneko.toneko.common.util.LanguageUtil.translatable;

public class CommonChatEvent {
    public static void onChatMessage(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound params) {
        NekoQuery.Neko neko = NekoQuery.getNeko(sender.getUUID());
        String msg = message.decoratedContent().getString();
        String playerName = TextUtil.getPlayerName(sender);
        // 获取昵称
        String nickname = neko.getNickName();
        // 修改消息
        msg = modify(msg, neko);
        // 格式化消息
        msg = Messaging.format(msg,playerName,nickname);
        // 消息中喵的数量
        int count = Stats.getMeow(msg);
        // 根据喵的数量增加经验
        neko.addLevel((double) count / 1000.00);
        if(ConfigUtil.STATS) Stats.meowInChat(playerName,count);
        sendMessage(Component.nullToEmpty(msg));
    }
    public static void sendMessage(Component message){
        for (Player player : PlayerUtil.getPlayerList()){
            player.sendSystemMessage(message);
        }
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
            Player player = PlayerUtil.getPlayerByUUID(UUID.fromString(uuid));
            if(player!=null){
                // 替换主人名称
                String name = TextUtil.getPlayerName(player);
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
