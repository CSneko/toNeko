package org.cneko.toneko.common;

import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.FileUtil;
import org.cneko.toneko.common.util.LanguageUtil;


public class Bootstrap {
    public static String MODID = "toneko";
    public static String DATA_PATH = "ctlib/toneko/";
    public static String PLAYER_DATA_PATH = DATA_PATH + "data/";
    public static org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("ToNeko");
    public static JsonConfiguration DEFAULT_PLAYER_PROFILE = JsonConfiguration.of("""
            {
              "uuid": "default",
              "is": false,
              "blockWords": [],
              "owners": []
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

    public static void bootstrap() {
        // 创建必要的文件夹
        FileUtil.CreatePath(DATA_PATH);
        FileUtil.CreatePath(PLAYER_DATA_PATH);
        // 加载配置文件
        ConfigUtil.load();
        // 加载语言文件
        LanguageUtil.load();
    }
}
