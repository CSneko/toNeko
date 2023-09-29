package com.crystalneko.toneko;

import com.crystalneko.toneko.command.ToNekoCommand;
import com.crystalneko.toneko.event.PlayerAttack;
import com.crystalneko.toneko.event.PlayerDeath;
import com.crystalneko.toneko.event.PlayerJoin;
import com.crystalneko.toneko.files.create;
import com.crystalneko.toneko.chat.nekoed;
import com.crystalneko.toneko.items.getStick;

import com.crystalneko.ctlib.chat.chatPrefix;

import org.bukkit.plugin.java.JavaPlugin;


public final class ToNeko extends JavaPlugin {
    private create createFile;
    private nekoed catedChat;
    private getStick getstick;
    private PlayerDeath playerDeath;
    private chatPrefix ChatPrefix;
    private PlayerJoin playerJoin;
    private PlayerAttack playerAttack;


    @Override
    public void onEnable() {
        int pluginId = 19899; // <-- Replace with the id of your plugin!
        Metrics metrics = new Metrics(this, pluginId);
        //初始化createFile
        createFile = new create();
        //创建数据保存文件
        createFile.createNewFile("plugins/toNeko/nekos.yml");
        //初始化聊天监听器
        this.catedChat = new nekoed(this);
        //初始化厥猫棍获取器
        this.getstick = new getStick(this);
        //初始化死亡监听器
        this.playerDeath = new PlayerDeath(this);
        //注册命令执行器
        getCommand("toneko").setExecutor(new ToNekoCommand(this,getstick));
        //注册玩家加入监听器
        this.playerJoin = new PlayerJoin(this);
        //注册玩家受到攻击监听器
        this.playerAttack = new PlayerAttack(this);

    }




    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
