package org.cneko.toneko.bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import org.cneko.toneko.bukkit.commands.ToNekoCommand;
import org.cneko.toneko.bukkit.events.ChatEvent;
import org.cneko.toneko.bukkit.events.PlayerConnectionEvents;
import org.cneko.toneko.bukkit.events.WorldEvents;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.FileUtil;
public class ToNeko extends JavaPlugin {
    public static JavaPlugin INSTANCE;
    @Override
    public void onEnable() {
        INSTANCE = this;
        FileUtil.CreatePath("plugins/toNeko");
        ConfigUtil.CONFIG_FILE = "plugins/toNeko/config.yml";
        Bootstrap.bootstrap();
        ToNekoCommand.init();
        ChatEvent.init();
        PlayerConnectionEvents.init();
        WorldEvents.init();
    }
}
