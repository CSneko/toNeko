package org.cneko.toneko.common.util;

import org.cneko.ctlib.common.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.util.LanguageUtil.language;

public class ConfigUtil {
    public static String CONFIG_FILE = "config/toneko.yml";
    public static String DEFAULT_CONFIG = """
            # 语言选项（支持 zh_cn,en_us）,可自定义语言，详细查看 https://github.com/CSneko/toNeko/docs/CUSTOM_LANGUAGE.md
            # Language Option (Supported zh_cn,en_us), you can customize the language, see https://github.com/CSneko/toNeko/docs/CUSTOM_LANGUAGE.md
            language: zh_cn
            # 聊天相关设置
            # Chat related settings
            chat:
              # 是否启用聊天修改 （开启后会对MCDr造成一定影响）
              # Whether to enable chat modification (it will cause some effects on MCDr)
              enable: true
              # 聊天格式，占位符 ${name} = 玩家名称, ${msg} = 玩家消息, ${prefix} = 前缀
              # Chat format, placeholder ${name} = player name, ${msg} = player message, ${prefix} = prefix
              format: "${prefix}§e${name} §6>> §f${msg}"
            # 启用统计功能，统计数据将发送到 toneko API，如何使用api请查看 https://github.com/CSneko/toNeko/blob/main/docs/TONEKO_ONLINE_API.md
            # Enable statistics, statistics data will be sent to the toneko API, how to use the api please see https://github.com/CSneko/toNeko/blob/main/docs/TONEKO_ONLINE_API.md
            stats: true
            """;

    public static YamlConfiguration CONFIG = YamlConfiguration.of(DEFAULT_CONFIG);
    public static boolean CHAT_ENABLE = true;
    public static String CHAT_FORMAT = "${prefix}§e${name} §6>> §f${msg}";
    public static String CHAT_TONE = "misc.toneko.nya";
    public static boolean STATS = true;
    public static boolean IS_BIRTHDAY = false;
    private static final int BIRTHDAY_MONTH = 9;
    private static final int BIRTHDAY_DAY = 26;
    public static Config INSTANCE;

    public static void load(){
        if (INSTANCE != null) {
            INSTANCE.load();
        }
        language = CONFIG.getString("language");
        CHAT_ENABLE = CONFIG.getBoolean("chat.enable",true);
        CHAT_FORMAT = CONFIG.getString("chat.format");
        CHAT_TONE = CONFIG.getString("chat.tone");
        STATS = CONFIG.getBoolean("stats",true);
        LocalDate today = LocalDate.now();
        if (today.getMonthValue() == BIRTHDAY_MONTH && today.getDayOfMonth() == BIRTHDAY_DAY) {
            IS_BIRTHDAY = true;
        }
    }

    // 预加载
    public static void preload(){
        if (Files.exists(Path.of(CONFIG_FILE))) {
            try {
                CONFIG = YamlConfiguration.fromFile(Path.of(CONFIG_FILE));
            } catch (IOException e) {
                LOGGER.error("Failed to load config file",e);
            }
        }
    }

    public interface Config {
        void load();
    }
}
