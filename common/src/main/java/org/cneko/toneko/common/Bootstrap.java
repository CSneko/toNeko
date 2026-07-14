package org.cneko.toneko.common;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.FileUtil;
import org.cneko.toneko.common.util.LanguageUtil;


public class Bootstrap {
    public static final String MODID = "toneko";
    public static String DATA_PATH = "ctlib/toneko/";
    public static String CONFIG_PATH = null; // Override set by Bukkit before bootstrap()
    public static org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("ToNeko");

    public static void bootstrap() {
        // Set config path before loading (Bukkit overrides this)
        if (CONFIG_PATH != null) {
            ConfigUtil.CONFIG_FILE = CONFIG_PATH;
        }

        FileUtil.CreatePath(DATA_PATH);
        ConfigUtil.load();
        LanguageUtil.load();
    }
}
