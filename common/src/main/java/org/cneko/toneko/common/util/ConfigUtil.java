package org.cneko.toneko.common.util;

import org.cneko.ctlib.common.file.YamlConfiguration;

import java.nio.file.Path;
import java.time.LocalDate;

public class ConfigUtil {
    public static String CONFIG_FILE = "config/toneko.yml";
    
    public static String AI_URL = "https://s.cneko.org/toNekoAI";

    public static final ConfigBuilder CONFIG_BUILDER = ConfigBuilder.create(Path.of(CONFIG_FILE))
            .addString("language", "zh_cn", null,
                    "语言选项（支持 zh_cn,zh_tw,en_us,ko_kr）,可自定义语言，详细查看 https://github.com/CSneko/toNeko/docs/CUSTOM_LANGUAGE.md",
                    "Language Option (Supported zh_cn,zh_tw,en_us,ko_kr), you can customize the language, see https://github.com/CSneko/toNeko/docs/CUSTOM_LANGUAGE.md")
            .addBoolean("chat.enable", true, null,
                    "是否启用聊天修改 （开启后会对MCDr造成一定影响）",
                    "Whether to enable chat modification (it will cause some effects on MCDr)" )
            .addString("chat.format","%prefix%§e%name% §d>> §f%msg%",null,
                    "聊天格式，占位符 %name% = 玩家名称, %msg% = 玩家消息, %prefix% = 前缀",
                    "Chat format, placeholder %name% = player name, %msg% = player message, %prefix% = prefix")
            .addBoolean("ai.enable",false,AI_URL,
                    "是否启用AI",
                    "Enable AI")
            .addString("ai.service","neko",AI_URL)
            .addString("ai.model", "gemini-2.0-flash",AI_URL)
            .addString("ai.key","",AI_URL,
                    "AI的密钥，请参阅https://s.cneko.org/toNekoAI",
                    "API Key of AI,see https://s.cneko.org/toNekoAI")
            .addString("ai.prompt","你是一只可爱的%neko_type%（%neko_des%），你的名字是%neko_name%，你的身高是%neko_height%米，你的属性有%neko_moe_tags%。在回复时请不要太长且不要有太多动作。我%player_is_owner%你的主人，我的名字是%player_name%，我%player_is_neko%一只猫娘。现在是%world_time%，天气为%world_weather%。",AI_URL,
                    "AI提示词，参阅 https://s.cneko.org/toNekoAI",
                    "AI prompt,see https://s.cneko.org/toNekoAI")
            .addBoolean("ai.show_think",true,AI_URL)
            .addBoolean("ai.proxy.enable",false,AI_URL)
            .addString("ai.proxy.ip","",AI_URL)
            .addString("ai.proxy.port","2080",AI_URL)
            .addBoolean("ai.tts.enable",false, AI_URL)
            .addString("ai.tts.service","elefant",AI_URL)
            .addString("ai.tts.voice","01955d76-ed5b-75ad-afe3-ac5eb3d0a16e",AI_URL)
            .addBoolean("stats", true, "https://s.cneko.org/toNekoOnlineAPI",
                    "启用统计功能，统计数据将发送到 toneko API，如何使用api请查看 https://s.cneko.org/toNekoOnlineAPI",
                    "Enable statistics, statistics data will be sent to the toneko API, how to use the api please see https://s.cneko.org/toNekoOnlineAPI")
            .build();
    public static ConfigBuilder.YamlC CONFIG = CONFIG_BUILDER.createConfig();

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
    public static boolean isAIEnabled() {
        return CONFIG.getBoolean("ai.enable");
    }
    public static String getAIPrompt() {
        return CONFIG.getString("ai.prompt");
    }
    public static String getAIKey(){
        return CONFIG.getString("ai.key");
    }
    public static String getAIModel(){
        return CONFIG.getString("ai.model");
    }
    public static String getAIService(){
        return CONFIG.getString("ai.service");
    }
    public static boolean isAIShowThink(){
        return CONFIG.getBoolean("ai.show_think");
    }
    public static boolean isAIProxyEnabled(){
        return CONFIG.getBoolean("ai.proxy.enable");
    }
    public static String getAIProxyIp(){
        return CONFIG.getString("ai.proxy.ip");
    }
    public static String getAIProxyPort(){
        return CONFIG.getString("ai.proxy.port");
    }

    public static boolean isAITTSEnabled(){
        return CONFIG.getBoolean("ai.tts.enable");
    }

    public static String getAITTSVoice(){
        return CONFIG.getString("ai.tts.voice");
    }


}
