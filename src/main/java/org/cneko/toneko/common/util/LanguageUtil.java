package org.cneko.toneko.common.util;

import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.ctlib.common.file.Resources;
import org.cneko.toneko.common.Bootstrap;

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
    public static Language INSTANCE;
    public static void load(){
        INSTANCE.load();
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

    public static interface Language {
        void load();
        default String translatable(String key){
            if(LANG.contains(key)){
                return LANG.getString(key);
            }else if(EN_US_LANG != null && !language.equals("en_us") && LANG.contains("en_us."+key)){
                return EN_US_LANG.getString("en_us."+key);
            }
            return key;
        }
        default String translatable(String key, Object[] args){
            return String.format(translatable(key), args);
        }
    }
}
