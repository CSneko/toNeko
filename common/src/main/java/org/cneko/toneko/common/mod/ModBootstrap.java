package org.cneko.toneko.common.mod;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.network.chat.Component;
import org.cneko.toneko.common.api.Messaging;
import org.cneko.toneko.common.mod.api.events.ChatEvents;
import org.cneko.toneko.common.mod.commands.arguments.NekoArgument;
import org.cneko.toneko.common.mod.events.CommonChatEvent;
import org.cneko.toneko.common.mod.util.PlayerUtil;
import org.cneko.toneko.common.util.scheduled.FabricSchedulerPoolImpl;
import org.cneko.toneko.common.util.scheduled.SchedulerPoolProvider;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class ModBootstrap {
    public static void bootstrap() {
        Messaging.GET_PLAYER_UUID_INSTANCE = PlayerUtil::getPlayerUUIDByName;
        Messaging.PREFIX_EVENT_INSTANCE = (player, prefixes) -> ChatEvents.CREATE_CHAT_PREFIXES.invoker().onCreate(PlayerUtil.getPlayerByName(player), prefixes);
        Messaging.SEND_MESSAGE_INSTANCE = (name,msg,modify)-> CommonChatEvent.sendMessage(Component.literal(msg));
        Messaging.NEKO_MODIFY_INSTANCE = CommonChatEvent::modify;
        Messaging.ON_FORMAT_INSTANCE = (message, playerName, nickname,prefixes, format)-> ChatEvents.ON_CHAT_FORMAT.invoker().onFormat(message, playerName, nickname, prefixes,format);
        SchedulerPoolProvider.INSTANCE = new FabricSchedulerPoolImpl();
        ArgumentTypeRegistry.registerArgumentType(
                toNekoLoc("neko"),
                NekoArgument.class, SingletonArgumentInfo.contextFree(NekoArgument::neko));
    }
}
