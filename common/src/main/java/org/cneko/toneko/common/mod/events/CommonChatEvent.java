package org.cneko.toneko.common.mod.events;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.Stats;
import org.cneko.toneko.common.mod.api.events.ChatEvents;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.util.PlayerUtil;
import org.cneko.toneko.common.mod.util.TextUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.util.LanguageUtil.translatable;

public class CommonChatEvent {
    public static void onChatMessage(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound params) {
        String msg = message.decoratedContent().getString();
        String playerName = TextUtil.getPlayerName(sender);
        // 修改消息
        msg = Messaging.nekoModify(msg, sender);
        // 格式化消息
        msg = Messaging.format(msg, sender, Messaging.getChatPrefixes(sender), Messaging.DEFAULT_CHAT_FORMAT);
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

    /**
     * 通用的聊天消息处理，具体的聊天消息处理逻辑由各个端的监听器实现
     */
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

    public static class Messaging {

        public static final String DEFAULT_CHAT_FORMAT = "%prefix%%name%: %msg%";
        private static final String PREFIX_FORMAT = "[§a%s§f§r]";

        public static void sendMessage(INeko sender, String message, boolean modify) {
            if (modify) {
                message = nekoModify(message, sender);
            }
            String formatted = format(message, sender, getChatPrefixes(sender), ConfigUtil.getChatFormat());
            CommonChatEvent.sendMessage(Component.nullToEmpty(formatted));
        }

        public static String format(String message, INeko sender, List<String> prefixes, String chatFormat) {
            String nickname = sender.getNickName();
            if (nickname.isBlank()) {
                nickname = sender.getEntity().getName().getString();
            } else {
                nickname = "§6~§f" + nickname;
            }
            // 事件钩子
            message = ChatEvents.ON_CHAT_FORMAT.invoker().onFormat(message, sender, prefixes, chatFormat);

            return chatFormat
                    .replace("%prefix%", formatPrefixes(prefixes))
                    .replace("%msg%", message)
                    .replace("%name%", nickname)
                    .replace("%c%", "§");
        }

        public static List<String> getChatPrefixes(INeko sender) {
            List<String> prefixes = new ArrayList<>();
            if (sender.isNeko()) {
                prefixes.add(LanguageUtil.prefix);
            }
            ChatEvents.CREATE_CHAT_PREFIXES.invoker().onCreate(sender, prefixes);
            return prefixes;
        }

        public static String replacePhrase(String message, String phrase) {
            return PhraseProcessor.runPetPhrases(message, phrase);
        }

        private static String formatPrefixes(List<String> prefixes) {
            StringBuilder formatted = new StringBuilder();
            for (String prefix : prefixes) {
                formatted.append(String.format(PREFIX_FORMAT, prefix));
            }
            return formatted.toString();
        }

        public static String nekoModify(String message, INeko neko) {
            for (INeko.BlockedWord block : neko.getBlockedWords()) {
                if (message.contains(block.block())) {
                    if (block.method() == INeko.BlockedWord.BlockMethod.ALL) {
                        message = block.replace();
                        break;
                    } else if (block.method() == INeko.BlockedWord.BlockMethod.WORD) {
                        message = message.replace(block.block(), block.replace());
                    }
                }
            }
            String phrase = translatable(LanguageUtil.phrase);
            message = replacePhrase(message, phrase);
            message = CommonChatEvent.modify(message, neko);
            return message;
        }

        public interface OnFormat {
            String onFormat(String msg, INeko sender, List<String> prefixes, String chatFormat);
        }

        public static class PhraseProcessor {
            private static final Set<Character> PUNCTUATIONS = Set.of('.', ',', '?', '!', '。', '，', '？', '！');
            private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();
            private static final double REPLACE_PROBABILITY = 0.4;

            public static String replaceCharWithRandom(String text, char targetChar, String replacement, double probability) {
                if (text.isEmpty() || probability <= 0) return text;
                return IntStream.range(0, text.length())
                        .collect(StringBuilder::new, (sb, i) -> {
                            char c = text.charAt(i);
                            if (c == targetChar && RANDOM.nextDouble() < probability) {
                                sb.append(replacement);
                            } else {
                                sb.append(c);
                            }
                        }, StringBuilder::append)
                        .toString();
            }

            public static String runPetPhrases(String text, String petPhrase) {
                if (text.isEmpty() || petPhrase.isEmpty()) return text;
                text = processTextEnding(text, petPhrase);
                return PUNCTUATIONS.parallelStream()
                        .filter(punct -> !petPhrase.endsWith(String.valueOf(punct)))
                        .reduce(text, (current, punct) ->
                                        replaceCharWithRandom(current, punct, petPhrase + punct, REPLACE_PROBABILITY),
                                (s1, s2) -> s2);
            }

            private static String processTextEnding(String text, String petPhrase) {
                return Optional.of(text)
                        .filter(t -> !t.endsWith(petPhrase))
                        .map(t -> {
                            char lastChar = t.charAt(t.length() - 1);
                            return PUNCTUATIONS.contains(lastChar)
                                    ? t.substring(0, t.length() - 1) + petPhrase + lastChar
                                    : t + petPhrase;
                        })
                        .orElse(text);
            }
        }
    }
}