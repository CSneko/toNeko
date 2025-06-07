package org.cneko.toneko.common.mod.misc;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.api.events.ChatEvents;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.events.CommonChatEvent;
import org.cneko.toneko.common.mod.util.PlayerUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.cneko.toneko.common.util.LanguageUtil.translatable;

public class Messaging {

    private static final String PREFIX_FORMAT = "[§a%s§f§r]";

    public static void modifyAndSendMessage(INeko sender,String message, Entity target) {
        message = nekoModify(message, sender);
        message = CommonChatEvent.modify(message, sender);
        String chatFormat = ConfigUtil.getChatFormat();
        List<String> prefixes = getChatPrefixes(sender);
        String formattedMessage = format(message, sender, prefixes, chatFormat);
        target.sendSystemMessage(Component.literal(formattedMessage));
    }

    public static void modifyAndSendMessageToAll(INeko sender, String message) {
        message = nekoModify(message, sender);
        message = CommonChatEvent.modify(message, sender);
        String chatFormat = ConfigUtil.getChatFormat();
        List<String> prefixes = getChatPrefixes(sender);
        String formattedMessage = format(message, sender, prefixes, chatFormat);
        for (Player player : PlayerUtil.getPlayerList()) {
            player.sendSystemMessage(Component.literal(message));
        }
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
        if (neko.isNeko()) {
            message = replacePhrase(message, phrase);
        }
        message = CommonChatEvent.modify(message, neko);
        return message;
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
