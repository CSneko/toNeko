package org.cneko.toneko.common.util;

import org.cneko.ctlib.common.util.ChatPrefix;

import java.util.List;
import java.util.Random;


public class Messaging {

    public static String format(String msg, String player, String nickname){
        return format(msg,player,nickname,ChatPrefix.getPrivatePrefix(player) + ChatPrefix.getAllPublicPrefixValues());
    }

    public static String format(String msg, String player, String nickname, String prefix){
        // 修改昵称
        if(nickname.isEmpty()){
            nickname = player;
        }else {
            nickname = "§6~§f"+nickname;
        }
        // 从config读取格式
        String format = ConfigUtil.CHAT_FORMAT;
        return format.
                replace("${prefix}",prefix).
                replace("${msg}",msg).
                replace("${name}",nickname).
                replace("${c}","§");
    }

    public static String replacePhrase(String message, String phrase){
        message = runPetPhrases(message, phrase);
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
}
