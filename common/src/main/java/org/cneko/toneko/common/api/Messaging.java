package org.cneko.toneko.common.api;

import org.cneko.toneko.common.api.json.NekoDataModel;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

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

    public static void sendMessage(String playerName, String message, boolean modify){
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
        message = runPetPhrases(message, phrase);
        return message;
    }

    private static String formatPrefixes(List<String> prefixes){
        StringBuilder formatted = new StringBuilder();
        for (String prefix : prefixes) {
            // 将每个前缀格式化为 [§a前缀§f§r]
            formatted.append("[§a").append(prefix).append("§f§r]");
        }
        return formatted.toString();
    }

    public static String nekoModify(String message, NekoQuery.Neko neko){
        List<NekoDataModel.Owner> owners= neko.getOwners();

        // 替换屏蔽词
        for (NekoDataModel.BlockWord block : neko.getProfile().getBlockWords()){
            if(block.getMethod() == NekoDataModel.BlockWord.Method.ALL && message.contains(block.getBlock())){
                // 如果屏蔽词的类型为all，则直接替换为屏蔽词
                message = block.getBlock();
                break;
            }
            message = message.replace(block.getBlock(),block.getBlock());
        }


        //添加口癖
        String phrase = LanguageUtil.phrase;
        phrase = translatable(phrase);
        message = Messaging.replacePhrase(message,phrase);
        // 修改消息
        message = NEKO_MODIFY_INSTANCE.modify(message,neko);
        return message;
    }

    /*
     以下代码来源于
     https://github.com/CSneko/kawai-text/blob/main/js/petPhrase.js
     */

    public static String replaceCharWithRandom(String text, char chr, String target, double probability) {
        String result = text;
        Random random = new Random();
        double rand = random.nextDouble();
        if (rand < probability) {
            int index = text.indexOf(chr);
            if (index != -1) {
                result = text.substring(0, index) + target + text.substring(index + 1);
            }
        }
        return result;
    }

    public static String runPetPhrases(String text, String petPhrase) {
        char[] punctuation = {'.', ',', '?', '!', '。', '，', '？', '！'};

        // Check and add petPhrase at the end if needed
        if (!text.endsWith(petPhrase)) {
            boolean add = true;
            for (char punct : punctuation) {
                if (text.endsWith(String.valueOf(punct))) {
                    add = false;
                    // Remove the last character
                    text = text.substring(0, text.length() - 1);
                    break;
                }
            }
            if (add) {
                text = text + petPhrase;
            }
        }

        // Add petPhrase before punctuation marks
        for (char punct : punctuation) {
            // If petPhrase does not end with punctuation mark, add it before
            if (!petPhrase.endsWith(String.valueOf(punct))) {
                text = replaceCharWithRandom(text, punct, petPhrase + punct, 0.4);
            }
        }

        return text;
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
