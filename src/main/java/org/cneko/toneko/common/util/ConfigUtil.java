package org.cneko.toneko.common.util;

import org.cneko.ctlib.common.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Path;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.util.LanguageUtil.language;

public class ConfigUtil {
    public static String CONFIG_FILE = "config/toneko.yml";
    public static String DEFAULT_CONFIG = """
            #语言选项（支持 zh_cn,en_us）,可自定义语言，详细查看 https://github.com/CSneko/toNeko/docs/CUSTOM_LANGUAGE.md
#Language Option (Supported zh_cn,en_us), you can customize the language, see https://github.com/CSneko/toNeko/docs/CUSTOM_LANGUAGE.md
language: zh_cn
#使用客户端语言，启用后语言选项部分无效，且要求玩家必须在客户端安装mod或者材质包才能正常显示消息
#Use client language, enable this option, the language option part is invalid, and the player must install the mod or the material package on the client to display the message normally
client-language: true
# 聊天相关设置
# Chat related settings
chat:
  # 是否启用聊天修改 （开启后会对MCDr造成一定影响）
  # Whether to enable chat modification (it will cause some effects on MCDr)
  enable: true
  # 聊天格式，占位符 ${name} = 玩家名称, ${msg} = 玩家消息, ${prefix} = 前缀
  # Chat format, placeholder ${name} = player name, ${msg} = player message, ${prefix} = prefix
  format: "${prefix}§e${name} §6>> §f${msg}"
            """;

    public static YamlConfiguration CONFIG = YamlConfiguration.of(DEFAULT_CONFIG);

    public static void load(){
        // 判断config文件是否存在
        if (!FileUtil.FileExists(CONFIG_FILE)) {
            // 创建config文件
            FileUtil.CreateFile(CONFIG_FILE);
            // 写入默认配置
            FileUtil.WriteFile(CONFIG_FILE, DEFAULT_CONFIG);
        }else {
            try {
                CONFIG = YamlConfiguration.fromFile(Path.of(CONFIG_FILE));
            } catch (IOException e) {
                LOGGER.error("Failed to load config file",e);
            }
        }
        language = CONFIG.getString("language");
    }
}
