package com.crystalneko.tonekonk;

import cn.nukkit.command.PluginCommand;
import cn.nukkit.plugin.PluginBase;
import com.crystalneko.tonekonk.commands.ToNekoCommand;
import com.crystalneko.tonekonk.bstats.Metrics;
import static org.cneko.ctlib.common.util.LocalDataBase.Connections.sqlite;

public class ToNekoNK extends PluginBase {
    @Override
    public void onEnable() {
        // 运行bStats
        new Metrics(this,19899);
        // 创建必要的sqlite
        sqlite.createTable("neko");
        sqlite.addColumn("neko", "name");
        sqlite.addColumn("neko", "owner");
        // 注册监听事件
        getServer().getPluginManager().registerEvents(new Events(), this);
        // 添加命令
        PluginCommand<ToNekoNK> toNekoCommand = new PluginCommand<>("toneko", this);
        toNekoCommand.setExecutor(new ToNekoCommand());
        getServer().getCommandMap().register("toneko", toNekoCommand);

    }
}
