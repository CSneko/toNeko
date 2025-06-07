package org.cneko.toneko.common.mod.events;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.Stats;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.misc.Messaging;
import org.cneko.toneko.common.mod.util.PlayerUtil;
import org.cneko.toneko.common.mod.util.TextUtil;
import org.cneko.toneko.common.util.ConfigUtil;

import java.util.*;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.util.LanguageUtil.translatable;

public class CommonChatEvent {
    public static void onChatMessage(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound params) {
        String msg = message.decoratedContent().getString();
        String playerName = TextUtil.getPlayerName(sender);
        // 修改消息
        msg = Messaging.nekoModify(msg, sender);
        // 格式化消息
        msg = Messaging.format(msg, sender, Messaging.getChatPrefixes(sender), ConfigUtil.getChatFormat());
        // 消息中喵的数量
        int count = Stats.getMeow(msg);
        // 根据喵的数量增加经验
        sender.setNekoLevel((float) (sender.getNekoLevel() +  count / 1000.00));
        if (ConfigUtil.isStatsEnable()) Stats.meowInChat(playerName, count);
        sendMessage(Component.nullToEmpty(msg));
    }

    public static void sendMessage(Component message) {
        for (Player player : PlayerUtil.getPlayerList()) {
            player.sendSystemMessage(message);
        }
        LOGGER.info(message.getString().replaceAll("§[0-9a-fk-or]", ""));
    }
    
    public static String modify(String message, INeko neko) {
        if (neko.isNeko()) {
            var owners = neko.getOwners();
            for (UUID uuid : owners.keySet()) {
                Player player = PlayerUtil.getPlayerByUUID(uuid);
                if (player != null) {
                    INeko.Owner owner = neko.getOwner(uuid);
                    String name = player.getName().getString();
                    String t = translatable("misc.toneko.owner");
                    message = message.replace(name, t);
                    for (String s : owner.getAliases()) {
                        message = message.replace(s, t);
                    }
                }
            }
        }
        return message;
    }

}