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

import static org.cneko.toneko.common.Bootstrap.LOGGER;

public class CommonChatEvent {

    public static void onChatMessage(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound params) {
        String originalContent = message.decoratedContent().getString();
        String playerName = TextUtil.getPlayerName(sender);

        // 1. 处理内容
        String processedContent = Messaging.prepareMessage(originalContent, sender);

        // 2. 应用格式（得到包含颜色代码的字符串）
        String finalString = Messaging.formatMessage(processedContent, sender);

        // 3. 统计与经验逻辑
        int meowCount = Stats.getMeow(finalString);
        sender.setNekoLevel((float) (sender.getNekoLevel() + meowCount / 1000.00));

        if (ConfigUtil.isStatsEnable()) {
            Stats.meowInChat(playerName, meowCount);
        }

        // 4. 构建带 Hover 的组件并发送
        Component finalComponent = Messaging.createComponentWithHover(finalString, sender);
        sendMessage(finalComponent);
    }

    public static void sendMessage(Component message) {
        for (Player player : PlayerUtil.getPlayerList()) {
            player.sendSystemMessage(message);
        }
        // 记录日志时移除颜色代码
        LOGGER.info(message.getString().replaceAll("§[0-9a-fk-or]", ""));
    }

    @Deprecated
    public static String modify(String message, INeko neko) {
        return Messaging.prepareMessage(message, neko);
    }
}