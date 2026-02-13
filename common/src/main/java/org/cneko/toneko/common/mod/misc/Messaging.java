package org.cneko.toneko.common.mod.misc;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.api.events.ChatEvents;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.util.PlayerUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cneko.toneko.common.util.LanguageUtil.translatable;

public class Messaging {

    private static final String PREFIX_FORMAT = "[§a%s§f§r]";

    /**
     * 修改消息并发送给指定实体
     */
    public static void modifyAndSendMessage(INeko sender, String message, Entity target) {
        // 1. 获取处理后的文本
        String processedMsg = prepareMessage(message, sender);
        String finalMsgString = formatMessage(processedMsg, sender);

        // 2. 构建带悬停事件的组件
        Component component = createComponentWithHover(finalMsgString, sender);

        // 3. 发送
        target.sendSystemMessage(component);
    }

    /**
     * 修改消息并发送给所有人
     */
    public static void modifyAndSendMessageToAll(INeko sender, String message) {
        String processedMsg = prepareMessage(message, sender);
        String finalMsgString = formatMessage(processedMsg, sender);

        // 构建组件
        Component component = createComponentWithHover(finalMsgString, sender);

        for (Player player : PlayerUtil.getPlayerList()) {
            player.sendSystemMessage(component);
        }
    }

    /**
     * 将格式化后的字符串转换为组件，并添加悬停显示真实名称的事件
     */
    public static Component createComponentWithHover(String formattedMessage, INeko sender) {
        String realName = sender.getEntity().getName().getString();

        // 使用 literal 包含格式化后的文本
        return Component.literal(formattedMessage)
                .withStyle(style -> style.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("§f" + realName))
                ));
    }

    // --- 核心处理逻辑 ---

    public static String prepareMessage(String message, INeko sender) {
        // 屏蔽词
        message = processBlockedWords(message, sender);

        // 口癖与替换
        if (sender.isNeko()) {
            String phrase = translatable(LanguageUtil.phrase);
            // 必须先处理口癖，再替换主人名，防止口癖插入破坏名字
            message = PhraseProcessor.runPetPhrases(message, phrase);
            message = replaceOwnerAliases(message, sender);
        }
        return message;
    }

    public static String formatMessage(String message, INeko sender) {
        String chatFormat = ConfigUtil.getChatFormat();
        List<String> prefixes = getChatPrefixes(sender);
        return format(message, sender, prefixes, chatFormat);
    }

    public static String format(String message, INeko sender, List<String> prefixes, String chatFormat) {
        String nickname = sender.getNickName();
        if (nickname.isBlank()) {
            nickname = sender.getEntity().getName().getString();
        } else {
            nickname = "§6~§f" + nickname;
        }

        message = ChatEvents.ON_CHAT_FORMAT.invoker().onFormat(message, sender, prefixes, chatFormat);

        return chatFormat
                .replace("%prefix%", formatPrefixes(prefixes))
                .replace("%msg%", message)
                .replace("%name%", nickname)
                .replace("%c%", "§");
    }

    private static String processBlockedWords(String message, INeko neko) {
        for (INeko.BlockedWord block : neko.getBlockedWords()) {
            if (message.contains(block.block())) {
                if (block.method() == INeko.BlockedWord.BlockMethod.ALL) {
                    return block.replace();
                } else if (block.method() == INeko.BlockedWord.BlockMethod.WORD) {
                    message = message.replace(block.block(), block.replace());
                }
            }
        }
        return message;
    }

    private static String replaceOwnerAliases(String message, INeko neko) {
        var owners = neko.getOwners();
        if (owners == null || owners.isEmpty()) return message;

        String ownerTitle = translatable("misc.toneko.owner");
        for (UUID uuid : owners.keySet()) {
            Player player = PlayerUtil.getPlayerByUUID(uuid);
            if (player != null) {
                String realName = player.getName().getString();
                message = message.replace(realName, ownerTitle);
                INeko.Owner ownerInfo = neko.getOwner(uuid);
                if (ownerInfo != null) {
                    for (String alias : ownerInfo.getAliases()) {
                        message = message.replace(alias, ownerTitle);
                    }
                }
            }
        }
        return message;
    }

    public static List<String> getChatPrefixes(INeko sender) {
        List<String> prefixes = new ArrayList<>();
        if (sender.isNeko()) {
            prefixes.add(LanguageUtil.prefix);
        }
        ChatEvents.CREATE_CHAT_PREFIXES.invoker().onCreate(sender, prefixes);
        return prefixes;
    }

    private static String formatPrefixes(List<String> prefixes) {
        if (prefixes.isEmpty()) return "";
        StringBuilder formatted = new StringBuilder();
        for (String prefix : prefixes) {
            formatted.append(String.format(PREFIX_FORMAT, prefix));
        }
        return formatted.toString();
    }

    @Deprecated
    public static String nekoModify(String message, INeko neko) {
        return prepareMessage(message, neko);
    }

    // --- 标点与口癖处理 ---

    public static class PhraseProcessor {
        // 匹配常见的标点符号组，例如 "..." 或 "!?"
        // 注意：+号表示匹配连续的一个或多个
        private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("([.,?!~。，？！～]+)");
        private static final double REPLACE_PROBABILITY = 0.4;

        public static String runPetPhrases(String text, String petPhrase) {
            if (text == null || text.isEmpty() || petPhrase == null || petPhrase.isEmpty()) return text;

            // 1. 分离句尾标点 (解决 xx!! 变成 xx!喵! 的问题)
            String body = "";
            String tail = "";

            Matcher matcher = PUNCTUATION_PATTERN.matcher(text);
            int lastMatchEnd = -1;

            // 寻找最后一个匹配项
            while (matcher.find()) {
                lastMatchEnd = matcher.end();
            }

            // 如果最后一个匹配项是在字符串末尾
            if (lastMatchEnd == text.length()) {
                // 回退查找该组标点的起始位置
                matcher.reset();
                while (matcher.find()) {
                    if (matcher.end() == text.length()) {
                        tail = matcher.group(); // 提取例如 "!!" 或 "..."
                        body = text.substring(0, matcher.start());
                        break;
                    }
                }
                if (tail.isEmpty()) body = text; // 理论上不会执行到这，除非逻辑错误
            } else {
                body = text;
            }

            // 2. 处理句中部分 (解决 .... 变成 .喵.喵 的问题)
            StringBuilder sb = new StringBuilder();
            Matcher bodyMatcher = PUNCTUATION_PATTERN.matcher(body);
            ThreadLocalRandom random = ThreadLocalRandom.current();

            while (bodyMatcher.find()) {
                // 如果随机命中，在标点组之前插入口癖 (例如 "Hello, world" -> "Hello~nya, world")
                if (random.nextDouble() < REPLACE_PROBABILITY) {
                    bodyMatcher.appendReplacement(sb, petPhrase + "$1");
                } else {
                    bodyMatcher.appendReplacement(sb, "$1");
                }
            }
            bodyMatcher.appendTail(sb);

            // 3. 组合：处理后的身体 + 口癖 + 原始句尾标点
            // "Hello!!" -> "Hello" + "~nya" + "!!"
            return sb.toString() + petPhrase + tail;
        }
    }
}