package org.cneko.toneko.bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import org.cneko.toneko.bukkit.commands.ToNekoAdminCommand;
import org.cneko.toneko.bukkit.commands.ToNekoCommand;
import org.cneko.toneko.bukkit.events.ChatEvent;
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
        getCommand("toneko").setExecutor(new ToNekoCommand());
        getCommand("tonekoadmin").setExecutor(new ToNekoAdminCommand());
        ChatEvent.init();
    }
}
