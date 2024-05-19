package org.cneko.toneko.common;

import io.netty.handler.ssl.JdkSslContext;
import org.cneko.ctlib.common.file.JsonConfiguration;
import org.cneko.toneko.common.util.FileUtil;

import java.util.logging.Logger;

public class Bootstrap {
    public static String DATA_PATH = "ctlib/toneko/";
    public static String PLAYER_DATA_PATH = DATA_PATH + "data/";
    public static String CONFIG_FILE = "config/toneko.yml";
    public static Logger LOGGER = Logger.getLogger("ToNeko");
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
                "xp": 0
            }""");
    public static void bootstrap() {
        // 创建必要的文件夹
        FileUtil.CreatePath(DATA_PATH);
        FileUtil.CreatePath(PLAYER_DATA_PATH);
    }
}
