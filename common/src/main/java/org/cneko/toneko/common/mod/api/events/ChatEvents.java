package org.cneko.toneko.common.mod.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.entities.INeko;

import java.util.List;

public class ChatEvents {
    /**
     * 创建聊天前缀事件<br>
     * args:<br>
     * - player: 玩家<br>
     * - prefixes: 聊天前缀列表<br>
     */
    public static Event<CreateChatPrefixes> CREATE_CHAT_PREFIXES = EventFactory.createArrayBacked(CreateChatPrefixes.class,
            (listeners) -> (player, prefixes) -> {
                for (CreateChatPrefixes listener : listeners) {
                    listener.onCreate(player, prefixes);
                }
            }
    );
    /**
     * 聊天格式化事件<br>
     * args:<br>
     * - message: 消息<br>
     * - playerName: 玩家名<br>
     * - nickname: 玩家昵称<br>
     * - prefixes: 聊天前缀列表<br>
     * - chatFormat: 聊天格式
     */
    public static Event<OnChatFormat> ON_CHAT_FORMAT = EventFactory.createArrayBacked(OnChatFormat.class,
            (listeners) -> (message, sender, prefixes, chatFormat) -> {
                for (OnChatFormat listener : listeners) {
                    message = listener.onFormat(message, sender, prefixes, chatFormat);
                }
                return message;
            }
    );

    public interface CreateChatPrefixes{
        void onCreate(INeko sender, List<String> prefixes);
    }
    public interface OnChatFormat{
        String onFormat(String message, INeko sender, List<String> prefixes, String chatFormat);
    }
}
