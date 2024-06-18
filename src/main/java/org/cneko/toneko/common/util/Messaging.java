package org.cneko.toneko.common.util;

import org.cneko.ctlib.common.util.ChatPrefix;

import java.util.Locale;

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
        // TODO: 完善口癖
        // 随机将",，"替换为"喵~"
        message = replaceChar(message, ',', phrase, 0.4);
        message = replaceChar(message, '，', phrase, 0.4);
        return message + phrase;
    }
}
