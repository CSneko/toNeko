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
    public static JsonConfiguration EN_US_LANG;
    public static String phrase = "";
    public static String prefix = "";
    public static void load(){
        try {
            language = CONFIG.getString("language");
            Resources resources = new Resources(Bootstrap.class);
            // 创建文件夹如果不存在
            FileUtil.CreatePath(LANG_PATH);
            // 删除旧语言文件
            FileUtil.DeleteFile(LANG_PATH+"en_us.json");
            FileUtil.DeleteFile(LANG_PATH+"zh_cn.json");
            FileUtil.DeleteFile(LANG_PATH+"ko_kr.json");
            // 复制语言文件
            resources.copyDirectoryFromJar("assets/toneko/lang",LANG_PATH);
            // 读取语言文件
            LANG = JsonConfiguration.fromFile(Path.of(LANG_PATH+language+".json"));
            EN_US_LANG = JsonConfiguration.fromFile(Path.of(LANG_PATH+"en_us.json"));
        } catch (Exception e) {
            LANG = JsonConfiguration.of("{}");
            LOGGER.error("Failed to load language file",e);
        }
        phrase = translatable(LANG.getString("misc.toneko.nya"));
        prefix = translatable(LANG.getString("misc.toneko.prefix"));
    }

    public static String translatable(String key){
        if(LANG.contains(key)){
            return LANG.getString(key);
        }else if(EN_US_LANG != null && !language.equals("en_us") && LANG.contains("en_us."+key)){
            return EN_US_LANG.getString("en_us."+key);
        }
        return key;
    }

    public static String translatable(String key, Object[] args) {
        return String.format(translatable(key), args);
    }
}
