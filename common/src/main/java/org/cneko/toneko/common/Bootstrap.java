package org.cneko.toneko.common;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.FileUtil;
import org.cneko.toneko.common.util.LanguageUtil;


public class Bootstrap {
    public static final String MODID = "toneko";
    public static String DATA_PATH = "ctlib/toneko/";
    public static org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("ToNeko");

    public static void bootstrap() {
        /*
         * 生日快乐，toNeko
         *    --- CrystalNeko 2024/09/26
         */

        // 创建必要的文件夹
        FileUtil.CreatePath(DATA_PATH);
        // 加载配置文件
        ConfigUtil.load();
        // 加载语言文件
        LanguageUtil.load();

    }
}
