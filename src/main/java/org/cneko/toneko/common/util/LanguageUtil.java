package org.cneko.toneko.common.util;

import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.ctlib.common.file.Resources;
import org.cneko.toneko.common.Bootstrap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.cneko.toneko.common.Bootstrap.DATA_PATH;
import static org.cneko.toneko.common.util.ConfigUtil.CONFIG;
import static org.cneko.toneko.common.Bootstrap.LOGGER;
public class LanguageUtil {
    public static String LANG_PATH = DATA_PATH+"lang/";
    public static String language = "en_us";
    public static JsonConfiguration LANG;
    public static String phrase = "";
    public static String prefix = "";
    public static void load(){
        try {
            language = CONFIG.getString("language");
            Resources resources = new Resources(Bootstrap.class);
            // 删除旧语言文件
            FileUtil.DeleteFile(LANG_PATH+"en_us.json");
            FileUtil.DeleteFile(LANG_PATH+"zh_cn.json");
            // 复制语言文件
            resources.copyDirectoryFromJar("assets/toneko/lang", LANG_PATH);
            // 读取语言文件
            LANG = JsonConfiguration.fromFile(Path.of(LANG_PATH+language+".json"));
        } catch (URISyntaxException | IOException e) {
            LANG = JsonConfiguration.of("{}");
            LOGGER.error("Failed to load language file",e);
        }
        phrase = translatable(LANG.getString("misc.toneko.nya"));
        prefix = translatable(LANG.getString("misc.toneko.prefix"));
    }

    public static String translatable(String key){
        if(LANG.contains(key)){
            return LANG.getString(key);
        }
        return key;
    }
}
