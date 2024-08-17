package org.cneko.toneko.bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.FileUtil;

public class ToNeko extends JavaPlugin {
    public static JavaPlugin pluginInstance;
    @Override
    public void onEnable() {
        pluginInstance = this;
        FileUtil.CreatePath("plugins/toNeko");
        ConfigUtil.CONFIG_FILE = "plugins/toNeko/config.yml";
        Bootstrap.bootstrap();
    }
}
