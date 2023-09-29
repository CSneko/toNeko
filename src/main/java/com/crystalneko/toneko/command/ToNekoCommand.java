package com.crystalneko.toneko.command;

import com.crystalneko.toneko.ToNeko;
import com.crystalneko.toneko.files.create;
import com.crystalneko.toneko.items.getStick;
import com.crystalneko.ctlib.chat.chatPrefix;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class ToNekoCommand implements CommandExecutor, TabCompleter {
    private ToNeko toNeko;
    private getStick getstick;
    private Map<Player, Boolean> confirmMap = new HashMap<>();
    public ToNekoCommand(ToNeko toNeko, getStick getstick){
        this.toNeko = toNeko;
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
                //创建数据文件实例
                File dataFile = new File("plugins/toNeko/nekos.yml");
                // 加载数据文件
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                //创建数据值
                Boolean created = create.createNewKey(args[1] + "." + "owner", player.getDisplayName(), data, dataFile);
                create.createNewKey(args[1] + "." + "xp", 0, data, dataFile);
                if (created) {
                    killPlayer(player,args[1]);
                    player.sendMessage("§a成功将玩家§6" + args[1] + "§a设置成一个猫娘，你成为了它的主人");
                    chatPrefix.addPrivatePrefix(player,"§a猫娘");
                } else {
                    player.sendMessage("§b它已经是一个猫娘了，它的主人是§6" + data.getString(args[1] + ".owner"));
                }
            }

        } else if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage("§b/toneko 帮助:\n§a/toneko help §b获取帮助\n§a/toneko player <玩家名> §b将一位玩家变成猫娘(但是你会被祭献)\n§a/toneko item §b获取撅猫棍\n§a/toneko remove <猫娘名称> §b删除猫娘§c（这是个危险操作，请谨慎使用）\n§a/toneko xp <猫娘名称> §b查看好感经验");
        } else if (args[0].equalsIgnoreCase("item")) {
            if (player.hasPermission("toneko.command.item")){
                getstick.getStick(player);
            }
        }else if (args.length == 2 && args[0].equalsIgnoreCase("remove")){
            if (confirmMap.getOrDefault(player, false)) {
                //创建数据文件实例
                File dataFile = new File("plugins/toNeko/nekos.yml");
                // 加载数据文件
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                //判断玩家是否为主人
                if(data.getString(args[1] + ".owner") != null) {
                    if (data.getString(args[1] + ".owner").equals(player.getDisplayName())) {
                        //获取被删除的猫娘对象
                        Player neko = Bukkit.getPlayer(args[1]);
                        if(neko != null) {
                            chatPrefix.subPrivatePrefix(neko,"§a猫娘");
                            create.setNullValue(dataFile,args[1] + ".owner");
                            create.setValue(args[1] + ".xp",0,dataFile);
                            player.sendMessage("§a你已经成功删除了猫娘§6" + args[1]);
                        } else {
                            player.sendMessage("§c猫娘不存在或不在线");
                        }
                    }else {
                        player.sendMessage("§c你不是" + args[1] +"的主人!");
                    }
                }
                confirmMap.remove(player);  // 执行完毕后移除确认状态
                return true;
            } else {
                // 发出确认提醒
                player.sendMessage("§c请再次输入该命令以确认执行(别管下面的那条信息，那是错误的),请输入: /toneko remove " + args[1]);
                confirmMap.put(player, true);
                return false;
            }
        }else if(args.length == 2 && args[0].equalsIgnoreCase("xp")){
            //创建数据文件实例
            File dataFile = new File("plugins/toNeko/nekos.yml");
            // 加载数据文件
            YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
            //判断玩家是否为主人
            if(data.getString(args[1] + ".owner") != null) {
                if (data.getString(args[1] + ".owner").equals(player.getDisplayName())) {
                    int xp = data.getInt(args[1] + ".xp");
                    player.sendMessage("§a你与§e" + args[1] + "§b的好感经验为§e" + xp);
                }else {
                    player.sendMessage("§c你不是" + args[1] +"的主人!");
                }
            } else {
                if(data.getString(args[1] + ".owner") != null) {
                    if (data.getString(args[1] + ".owner").equals(player.getDisplayName())) {
                        create.createNewKey(args[1] + "." + "xp", player.getDisplayName(), data, dataFile);
                    }
                } else {
                    player.sendMessage("§c你不是" + args[1] +"的主人!");
                }
            }
        } else {
            player.sendMessage("§c无效的子命令,请输入§a/toneko help§c查看帮助");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("help");
            completions.add("player");
            completions.add("item");
            completions.add("remove");
            completions.add("xp");
            return completions;
        }
        return Collections.emptyList();
    }
    //祭献操作
    public void killPlayer(Player player, String neko) {
        //死亡提示
        Bukkit.getServer().broadcastMessage("§e玩家§6" + player.getDisplayName() + "§e为了使§6" + neko + "§e成为猫娘而祭献了自己");
        // 执行玩家死亡操作
        player.setHealth(0);
    }
}
