package org.cneko.toneko.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

public class ToNeko extends JavaPlugin {
    public static JavaPlugin pluginInstance;
    @Override
    public void onEnable() {
        pluginInstance = this;
    }
}
