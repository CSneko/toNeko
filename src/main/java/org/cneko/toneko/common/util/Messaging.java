package org.cneko.toneko.common.util;

import org.cneko.ctlib.common.util.ChatPrefix;

import java.util.Random;

import static org.cneko.toneko.common.util.StringUtil.replaceChar;

public class Messaging {

    public static String format(String msg, String player){
        // 从config读取格式
        String format = ConfigUtil.CHAT_FORMAT;
        String prefix = ChatPrefix.getPrivatePrefix(player) + ChatPrefix.getAllPublicPrefixValues();
        return format.
                replace("${prefix}",prefix).
                replace("${msg}",msg).
                replace("${name}",player).
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
