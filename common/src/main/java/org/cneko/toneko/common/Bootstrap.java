package org.cneko.toneko.common;
import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.toneko.common.api.NekoQuery;
import org.cneko.toneko.common.quirks.Quirks;
import org.cneko.toneko.common.util.AIUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.FileUtil;
import org.cneko.toneko.common.util.LanguageUtil;

import java.util.List;


public class Bootstrap {
    public static final String MODID = "toneko";
    public static String DATA_PATH = "ctlib/toneko/";
    public static String PLAYER_DATA_PATH = DATA_PATH + "data/";
    public static String SKIN_FILE = DATA_PATH + "skins.yml";
    public static org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("ToNeko");
    public static JsonConfiguration DEFAULT_PLAYER_PROFILE = JsonConfiguration.of("""
            {
              "uuid": "default",
              "is": false,
              "level": 0.00,
              "blockWords": [],
              "owners": [],
              "quirks":[]
            }""");
    public static JsonConfiguration DEFAULT_OWNER_PROFILE = JsonConfiguration.of("""
            {
                "uuid": "default",
                "xp": 0,
                "aliases": []
            }""");
    public static JsonConfiguration DEFAULT_BLOCK_WORDS = JsonConfiguration.of("""
            {
                "block": "default",
                "replace": "default",
                "method": "word"
            }""");
    public static List<String> DEFAULT_NEKO_SKINS = List.of("anjqstar","wtpluvver","_BeeTheCat_","McmMud","xips190","George107","KamciuOfficial","Aoi235513",
            "Frankownicka","Ydonaa","croissant_cat","bugfishh","M_HUA","Hentay_");

    public static void bootstrap() {
        /*
         * 生日快乐，toNeko
         *    --- CrystalNeko 2024/09/26
         */

        // 创建必要的文件夹
        FileUtil.CreatePath(DATA_PATH);
        FileUtil.CreatePath(PLAYER_DATA_PATH);
        // 加载配置文件
        ConfigUtil.load();
        // 加载语言文件
        LanguageUtil.load();
        // 注册所有癖好
        Quirks.init();
        // 初始化AI
        AIUtil.init();
        NekoQuery.NekoData.startAsyncAutoSave();
    }
}
