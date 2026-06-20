package org.cneko.toneko.common.mod.events;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.Stats;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.misc.Messaging;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.mod.util.PlayerUtil;
import org.cneko.toneko.common.mod.util.TextUtil;
import org.cneko.toneko.common.util.AIUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;

import java.util.Collections;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

public class CommonChatEvent {

    private static final double AREA_RANGE = 64.0;
    private static final double NEKO_AI_RANGE = 16.0;

    public static void onChatMessage(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound params) {
        String originalContent = message.decoratedContent().getString();
        String playerName = TextUtil.getPlayerName(sender);

        // 1. 处理内容
        String processedContent = Messaging.prepareMessage(originalContent, sender);

        // 2. 应用格式
        String finalString = Messaging.formatMessage(processedContent, sender);

        // 2.5 区域模式加头衔
        boolean areaMode = ToNekoNetworkEvents.isPlayerAreaChat(sender.getUUID());
        if (areaMode) {
            finalString = "§a[§f区域§a]§r " + finalString;
        }

        // 3. 统计与经验逻辑
        int meowCount = Stats.getMeow(finalString);
        org.cneko.toneko.common.mod.api.NekoLevelRegistry.interaction().addRaw(sender, meowCount / 1000.0);

        if (ConfigUtil.isStatsEnable()) {
            Stats.meowInChat(playerName, meowCount);
        }

        // 4. 消息发送：区域模式仅附近玩家，全服模式所有玩家
        Component finalComponent = Messaging.createComponentWithHover(finalString, sender);

        if (areaMode) {
            sendMessageInRange(finalComponent, sender, AREA_RANGE);
            // 区域模式下触发猫娘AI（16格内）
            processAreaChatAI(originalContent, sender);
        } else {
            sendMessage(finalComponent);
            // 全服模式下只有前缀匹配才触发AI
            processProximityChat(originalContent, sender);
        }
    }

    /** Send message only to players within range of the sender */
    private static void sendMessageInRange(Component message, ServerPlayer sender, double range) {
        for (Player player : PlayerUtil.getPlayerList()) {
            if (player == sender || sender.distanceToSqr(player) <= range * range) {
                player.sendSystemMessage(message);
            }
        }
        LOGGER.info(message.getString().replaceAll("§[0-9a-fk-or]", ""));
    }

    /** Area chat: trigger neko AI for messages within 16 blocks (no prefix needed) */
    private static void processAreaChatAI(String message, ServerPlayer sender) {
        if (!ConfigUtil.isAIEnabled()) return;
        if (message.isEmpty()) return;

        NekoEntity neko = EntityUtil.findNearestNekoEntity(sender, sender.level(), (float) NEKO_AI_RANGE);
        if (neko == null) return;

        AIUtil.sendMessage(neko.getUUID(), sender.getUUID(), neko.generateAIPrompt(sender), message, response -> {
            String r = Messaging.format(response.getResponse(), neko,
                    Collections.singletonList(LanguageUtil.prefix), ConfigUtil.getChatFormat());
            sender.sendSystemMessage(Component.literal(r));
        });
    }

    /** Proximity chat: trigger neko AI only when message starts with configured prefix */
    private static void processProximityChat(String message, ServerPlayer sender) {
        if (!ConfigUtil.isAIEnabled()) return;

        String prefix = ConfigUtil.getAIChatPrefix();
        if (prefix == null || prefix.isEmpty()) return;
        if (!message.startsWith(prefix)) return;

        String aiMessage = message.substring(prefix.length()).trim();
        if (aiMessage.isEmpty()) return;

        NekoEntity neko = EntityUtil.findNearestNekoEntity(sender, sender.level(), (float) NEKO_AI_RANGE);
        if (neko == null) return;

        AIUtil.sendMessage(neko.getUUID(), sender.getUUID(), neko.generateAIPrompt(sender), aiMessage, response -> {
            String r = Messaging.format(response.getResponse(), neko,
                    Collections.singletonList(LanguageUtil.prefix), ConfigUtil.getChatFormat());
            sender.sendSystemMessage(Component.literal(r));
        });
    }

    public static void sendMessage(Component message) {
        for (Player player : PlayerUtil.getPlayerList()) {
            player.sendSystemMessage(message);
        }
        LOGGER.info(message.getString().replaceAll("§[0-9a-fk-or]", ""));
    }

    @Deprecated
    public static String modify(String message, INeko neko) {
        return Messaging.prepareMessage(message, neko);
    }
}
