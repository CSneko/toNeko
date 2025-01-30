package org.cneko.toneko.common.mod;

import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.api.Messaging;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.mod.api.events.ChatEvents;
import org.cneko.toneko.common.mod.events.CommonChatEvent;
import org.cneko.toneko.common.mod.util.PlayerUtil;
import org.cneko.toneko.common.util.scheduled.FabricSchedulerPoolImpl;
import org.cneko.toneko.common.util.scheduled.SchedulerPoolProvider;

public class ModBootstrap {
    public static void bootstrap() {
        Messaging.GET_PLAYER_UUID_INSTANCE = PlayerUtil::getPlayerUUIDByName;
        Messaging.PREFIX_EVENT_INSTANCE = (player, prefixes) -> ChatEvents.CREATE_CHAT_PREFIXES.invoker().onCreate(PlayerUtil.getPlayerByName(player), prefixes);
        Messaging.SEND_MESSAGE_INSTANCE = (name,msg,modify)->{
            if (modify){
                msg = Messaging.nekoModify(msg, NekoQuery.getNeko(PlayerUtil.getPlayerUUIDByName(name)));
            }
            msg = Messaging.format(msg,name,NekoQuery.getNeko(PlayerUtil.getPlayerUUIDByName(name)).getNickName(),Messaging.getChatPrefixes(name));
            CommonChatEvent.sendMessage(Component.literal(msg));
        };
        Messaging.NEKO_MODIFY_INSTANCE = CommonChatEvent::modify;
        Messaging.ON_FORMAT_INSTANCE = (message, playerName, nickname,prefixes, format)-> ChatEvents.ON_CHAT_FORMAT.invoker().onFormat(message, playerName, nickname, prefixes,format);
        SchedulerPoolProvider.INSTANCE = new FabricSchedulerPoolImpl();
    }
}
