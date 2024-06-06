package org.cneko.toneko.fabric.events;


import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.toneko.common.Stats;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.scheduled.SchedulerPoolProvider;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.common.util.Messaging;
import org.cneko.toneko.fabric.util.PlayerUtil;
import org.cneko.toneko.fabric.util.TextUtil;

import java.util.List;
import java.util.UUID;
import static org.cneko.toneko.common.util.LanguageUtil.translatable;
public class ChatEvent {
    public static void init() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            SchedulerPoolProvider.getINSTANCE().executeAsync(() -> onChatMessage(message,sender,params));
            return false;
        });
    }

    public static void onChatMessage(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params) {
        NekoQuery.Neko neko = NekoQuery.getNeko(sender.getUuid());
        String msg = message.getContent().getString();
        String playerName = TextUtil.getPlayerName(sender);
        // 修改消息
        msg = modify(msg, neko);
        // 格式化消息
        msg = Messaging.format(msg,playerName);
        if(ConfigUtil.STATS) Stats.meowInChat(playerName,msg);
        sendMessage(Text.of(msg));
    }
    public static void sendMessage(Text message){
        for (PlayerEntity player : PlayerUtil.getPlayerList()){
            player.sendMessage(message);
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
        List<JsonConfiguration> owners= neko.getOwners().toJsonList();

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
            PlayerEntity player = PlayerUtil.getPlayerByUUID(UUID.fromString(uuid));
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
        Messaging.PetPhrase petPhrase = new Messaging.PetPhrase(phrase, false, LanguageUtil.phrase.length());
        message = petPhrase.addPhrase(message);
        return message;
    }

}
