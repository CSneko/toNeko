package org.cneko.toneko.common.util;

import java.nio.file.Path;
import java.time.LocalDate;

public class ConfigUtil {
    public static String CONFIG_FILE = "config/toneko.yml";

    public static ConfigBuilder CONFIG_BUILDER = ConfigBuilder.create(Path.of(CONFIG_FILE))
            .addString("language", "zh_cn",
                    "语言选项（支持 zh_cn,zh_tw,en_us,ko_kr）,可自定义语言，详细查看 https://github.com/CSneko/toNeko/docs/CUSTOM_LANGUAGE.md",
                    "Language Option (Supported zh_cn,zh_tw,en_us,ko_kr), you can customize the language, see https://github.com/CSneko/toNeko/docs/CUSTOM_LANGUAGE.md")
            .addBoolean("chat.enable", true,
                    "  是否启用聊天修改 （开启后会对MCDr造成一定影响）",
                    "Whether to enable chat modification (it will cause some effects on MCDr)" )
            .addString("chat.format","${prefix}§e${name} §d>> §f${msg}",
                    "聊天格式，占位符 ${name} = 玩家名称, ${msg} = 玩家消息, ${prefix} = 前缀",
                    "Chat format, placeholder ${name} = player name, ${msg} = player message, ${prefix} = prefix")
            .addBoolean("stats", true,
                    "启用统计功能，统计数据将发送到 toneko API，如何使用api请查看 https://github.com/CSneko/toNeko/blob/main/docs/TONEKO_ONLINE_API.md",
                    "Enable statistics, statistics data will be sent to the toneko API, how to use the api please see https://github.com/CSneko/toNeko/blob/main/docs/TONEKO_ONLINE_API.md")
            .build();
    public static ConfigBuilder.YC CONFIG = CONFIG_BUILDER.createConfig();

    public static boolean IS_BIRTHDAY = false;
    private static final int BIRTHDAY_MONTH = 9;
    private static final int BIRTHDAY_DAY = 26;

    public static void load(){
        CONFIG = CONFIG_BUILDER.createConfig();
        LocalDate today = LocalDate.now();
        if (today.getMonthValue() == BIRTHDAY_MONTH && today.getDayOfMonth() == BIRTHDAY_DAY) {
            IS_BIRTHDAY = true;
        }
    }

    public static boolean isChatEnable() {
        return CONFIG.getBoolean("chat.enable");
    }
    public static String getChatFormat() {
        return CONFIG.getString("chat.format");
    }
    public static boolean isStatsEnable() {
        return CONFIG.getBoolean("stats");
    }


}
