package org.cneko.toneko.common.api;

import org.cneko.toneko.common.api.json.NekoDataModel;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.cneko.toneko.common.util.LanguageUtil.translatable;

public class Messaging {
    @ApiStatus.Internal
    public static GetPlayerUUID GET_PLAYER_UUID_INSTANCE;
    @ApiStatus.Internal
    public static PrefixEvent PREFIX_EVENT_INSTANCE;
    @ApiStatus.Internal
    public static SendMessage SEND_MESSAGE_INSTANCE;
    @ApiStatus.Internal
    public static NekoModify NEKO_MODIFY_INSTANCE;
    @ApiStatus.Internal
    public static OnFormat ON_FORMAT_INSTANCE;
    private static final String PREFIX_FORMAT = "[§a%s§f§r]";

    public static void sendMessage(String playerName, String message, boolean modify){
        UUID uuid = GET_PLAYER_UUID_INSTANCE.get(playerName);
        if (uuid == null) return;
        NekoQuery.Neko neko = NekoQuery.getNeko(uuid);
        if (modify){
            message = Messaging.nekoModify(message, neko);
        }
        message = Messaging.format(message,playerName,neko.getNickName(),Messaging.getChatPrefixes(playerName));
        SEND_MESSAGE_INSTANCE.sendMessage(playerName,message,modify);
    }

    public static String format(String msg, String player, String nickname){
        return format(msg,player,nickname, getChatPrefixes(player));
    }

    public static String format(String message, String playerName, String nickname, String format) {
        return format(message,playerName,nickname,getChatPrefixes(playerName),format);
    }

    public static String format(String msg, String player, String nickname, List<String> prefix, String chatFormat){
        // 修改昵称
        if(nickname.isEmpty() || nickname.isBlank()){
            nickname = player;
        }else {
            nickname = "§6~§f"+nickname;
        }
        msg = ON_FORMAT_INSTANCE.format(msg,player,nickname,prefix,chatFormat);
        return chatFormat.
                replace("%prefix%",formatPrefixes(prefix)).
                replace("%msg%",msg).
                replace("%name%",nickname).
                replace("%c%","§");
    }

    public static String format(String msg, String player, String nickname, List<String> prefixes){
        return format(msg,player,nickname,prefixes, ConfigUtil.getChatFormat());
    }

    public static List<String> getChatPrefixes(String playerName){
        UUID uuid = GET_PLAYER_UUID_INSTANCE.get(playerName);
        List<String> prefixes = new ArrayList<>();
        if (NekoQuery.isNeko(uuid)){
            prefixes.add(LanguageUtil.prefix);
        }
        PREFIX_EVENT_INSTANCE.onPrefix(playerName,prefixes);
        return prefixes;
    }

    public static String replacePhrase(String message, String phrase){
        message = PhraseProcessor.runPetPhrases(message,phrase);
        return message;
    }

    private static String formatPrefixes(List<String> prefixes){
        StringBuilder formatted = new StringBuilder();
        for (String prefix : prefixes) {
            // 将每个前缀格式化为 [§a前缀§f§r]
            formatted.append(String.format(PREFIX_FORMAT, prefix));
        }
        return formatted.toString();
    }

    public static String nekoModify(String message, NekoQuery.Neko neko){
        List<NekoDataModel.Owner> owners= neko.getOwners();

        // 替换屏蔽词
        for (NekoDataModel.BlockWord block : neko.getProfile().getBlockWords()){
            if (message.contains(block.getBlock())) {
                if (block.getMethod() == NekoDataModel.BlockWord.Method.ALL) {
                    // 如果屏蔽词的类型为all，则直接替换为屏蔽词
                    message = block.getBlock();
                    message = message.replace(block.getBlock(),block.getReplace());
                    break;
                } else if (block.getMethod() == NekoDataModel.BlockWord.Method.WORD) {
                    message = message.replace(block.getBlock(), block.getReplace());
                }
            }
        }


        //添加口癖
        String phrase = LanguageUtil.phrase;
        phrase = translatable(phrase);
        message = Messaging.replacePhrase(message,phrase);
        // 修改消息
        message = NEKO_MODIFY_INSTANCE.modify(message,neko);
        return message;
    }

    public static class PhraseProcessor {
        // 标点符号常量池
        private static final Set<Character> PUNCTUATIONS = Set.of(
                '.', ',', '?', '!', '。', '，', '？', '！'
        );

        // 线程安全随机数
        private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

        // 概率阈值
        private static final double REPLACE_PROBABILITY = 0.4;

        /**
         * 智能字符替换 (原子操作版)
         * @param text 原始文本
         * @param targetChar 目标字符
         * @param replacement 替换内容
         * @param probability 替换概率 (0.0-1.0)
         * @return 处理后的文本
         */
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

        /**
         * 口癖处理引擎 (并行优化版)
         * @param text 原始文本
         * @param petPhrase 口癖短语
         * @return 处理后的文本
         */
        public static String runPetPhrases(String text, String petPhrase) {
            if (text.isEmpty() || petPhrase.isEmpty()) return text;

            // 阶段1：结尾处理 (保留标点)
            text = processTextEnding(text, petPhrase);

            // 阶段2：标点前插入 (并行流优化)
            return PUNCTUATIONS.parallelStream()
                    .filter(punct -> !petPhrase.endsWith(String.valueOf(punct)))
                    .reduce(text, (current, punct) ->
                                    replaceCharWithRandom(current, punct, petPhrase + punct, REPLACE_PROBABILITY),
                            (s1, s2) -> s2 // 合并策略选择最后处理结果
                    );
        }

        /**
         * 智能结尾处理
         */
        private static String processTextEnding(String text, String petPhrase) {
            return Optional.of(text)
                    .filter(t -> !t.endsWith(petPhrase))
                    .map(t -> {
                        char lastChar = t.charAt(t.length() - 1);
                        return PUNCTUATIONS.contains(lastChar)
                                ? t.substring(0, t.length() - 1) + petPhrase + lastChar // 保留原标点
                                : t + petPhrase;
                    })
                    .orElse(text);
        }
    }

    public interface GetPlayerUUID {
        UUID get(String playerName);
    }
    public interface PrefixEvent{
        void onPrefix(String playerName, List<String> prefix);
    }
    public interface SendMessage{
        void sendMessage(String playerName, String message, boolean modify);
    }
    public interface NekoModify{
        String modify(String message, NekoQuery.Neko neko);
    }
    public interface OnFormat{
        String format(String message, String playerName, String nickname, List<String> prefixes,String chatFormat);
    }
}
