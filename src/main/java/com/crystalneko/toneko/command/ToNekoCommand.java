package com.crystalneko.toneko.command;

import com.crystalneko.toneko.ToNeko;
import com.crystalneko.toneko.files.create;
import com.crystalneko.toneko.items.getStick;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ToNekoCommand implements CommandExecutor, TabCompleter {
    private ToNeko toNeko;
    private create createFile;
    private getStick getstick;
    public ToNekoCommand(ToNeko toNeko, create createFile, getStick getstick){
        this.toNeko = toNeko;
        this.createFile = createFile;
        this.getstick = getstick;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return true;
        }

        Player player = (Player) sender;

        // 处理子命令
        if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
            if (player.hasPermission("toneko.command")) {
                //创建一个新的玩家为cats
                //创建数据文件实例
                File dataFile = new File("plugins/toNeko/nekos.yml");
                // 加载数据文件
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                //创建数据值
                Boolean created = createFile.createNewKey(args[1] + "." + "owner", player.getDisplayName(), data, dataFile);
                if (created) {
                    killPlayer(player,"玩家 " + player.getName() + " 为了将玩家 " + args[1] + " 变成猫娘而祭献了自己");
                    player.sendMessage("§a成功将玩家§6" + args[1] + "§a设置成一个猫娘，你成为了它的主人");
                } else {
                    player.sendMessage("§b它已经是一个猫娘了，它的主人是§6" + data.getString(args[1] + ".owner"));
                }
            }

        } else if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage("§b/toneko 帮助:\n§a/toneko help §b获取帮助\n§a/toneko player <玩家名> §b将一位玩家变成猫娘(但是你会被祭献)");
        } else if (args[0].equalsIgnoreCase("item")) {
            if (player.hasPermission("toneko.command.item")){
                getstick.getStick(player);
            }
        }else {
            player.sendMessage("§c无效的子命令,请输入§a/toneko help§c查看帮助");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("player");
            completions.add("help");
            completions.add("item");

            return completions;
        }
        return Collections.emptyList();
    }
    //祭献操作
    public void killPlayer(Player player, String deathMessage) {
        // 修改玩家的死亡提示消息
        player.setCustomName( deathMessage);

        // 执行玩家死亡操作
        player.setHealth(0);

        // 立即重生玩家
        player.spigot().respawn();
    }



}
