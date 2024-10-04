package org.cneko.toneko.bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import org.cneko.toneko.bukkit.commands.NekoCommand;
import org.cneko.toneko.bukkit.commands.ToNekoAdminCommand;
import org.cneko.toneko.bukkit.commands.ToNekoCommand;
import org.cneko.toneko.bukkit.events.ChatEvent;
import org.cneko.toneko.bukkit.events.PlayerConnectionEvents;
import org.cneko.toneko.bukkit.events.WorldEvents;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.mod.impl.FabricConfigImpl;
import org.cneko.toneko.common.mod.impl.FabricLanguageImpl;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.FileUtil;
import org.cneko.toneko.common.util.LanguageUtil;

import static org.cneko.toneko.common.Bootstrap.MODID;

public class ToNeko extends JavaPlugin {
    public static JavaPlugin INSTANCE;
    @Override
    public void onEnable() {
        INSTANCE = this;
        FileUtil.CreatePath("plugins/toNeko");
        ConfigUtil.CONFIG_FILE = "plugins/toNeko/config.yml";
        ConfigUtil.INSTANCE = new FabricConfigImpl();
        LanguageUtil.INSTANCE = new FabricLanguageImpl();
        Bootstrap.bootstrap();
        ToNekoCommand.init();
        ToNekoAdminCommand.init();
        NekoCommand.init();
        ChatEvent.init();
        PlayerConnectionEvents.init();
        WorldEvents.init();
    }
}
