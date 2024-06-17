package com.crystalneko.tonekonk;

import cn.nukkit.command.PluginCommand;
import cn.nukkit.plugin.PluginBase;
import com.crystalneko.tonekonk.commands.ToNekoCommand;
import com.crystalneko.tonekonk.bstats.Metrics;
import org.cneko.ctlib.common.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ToNekoNK extends PluginBase {
    @Override
    public void onEnable() {
        // 运行bStats
        new Metrics(this,19899);
        // 创建必要的yaml文件
        Path nekoFile = Path.of("plugins/toNeko/neko.yml");
        if(!Files.exists(nekoFile)){
            try {
                Files.createDirectories(Path.of("plugins/toNeko/"));
                Files.createFile(nekoFile);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        // 注册监听事件
        getServer().getPluginManager().registerEvents(new Events(), this);
        // 添加命令
        PluginCommand<ToNekoNK> toNekoCommand = new PluginCommand<>("toneko", this);
        toNekoCommand.setExecutor(new ToNekoCommand());
        getServer().getCommandMap().register("toneko", toNekoCommand);

    }
}
