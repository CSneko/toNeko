package com.crystalneko.toneko.command;

import com.crystalneko.toneko.ToNeko;
import com.crystalneko.toneko.files.create;
import com.crystalneko.toneko.items.getStick;
import com.crystalneko.ctlib.chat.chatPrefix;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
            if (player.hasPermission("toneko.command.player")) {
                //创建数据文件实例
                File dataFile = new File("plugins/toNeko/nekos.yml");
                // 加载数据文件
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                //创建数据值
                Boolean created = create.createNewKey(args[1] + "." + "owner", player.getName(), data, dataFile);
                create.createNewKey(args[1] + "." + "xp", 0, data, dataFile);
                List<String> aliases = new ArrayList<>();
                aliases.add("胡桃");//我是胡桃的狗!!!!
                create.createNewKey(args[1] + "." + "aliases", aliases, data, dataFile);
                if (created) {
                    killPlayer(player,args[1]);
                    player.sendMessage("§a成功将玩家§6" + args[1] + "§a设置成一个猫娘，你成为了它的主人");
                    //判断猫娘是否在线
                    if(isPlayerOnline(args[1])) {
                        Player neko = Bukkit.getPlayer(args[1]);
                        chatPrefix.addPrivatePrefix(neko, "§a猫娘");
                    }
                } else {
                    player.sendMessage("§b它已经是一个猫娘了，它的主人是§6" + data.getString(args[1] + ".owner"));
                }
            }else {
                player.sendMessage("§c你没有执行该命令的权限!");
            }

        } else if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage("§b/toneko 帮助:\n§a/toneko help §b获取帮助\n§a/toneko player <玩家名> §b将一位玩家变成猫娘(但是你会被祭献)\n§a/toneko item §b获取撅猫棍\n§a/toneko remove <猫娘名称> §b删除猫娘§c（这是个危险操作，请谨慎使用）\n§a/toneko xp <猫娘名称> §b查看好感经验\n§a/toneko aliases <猫娘名称> add或remove <别名> §b为你添加或删除别名（会转换为'主人'的词）");
        } else if (args[0].equalsIgnoreCase("item")) {
            if (player.hasPermission("toneko.command.item")){
                getstick.getStick(player);
            }else {
                player.sendMessage("§c你没有执行该命令的权限!");
            }
        }else if (args.length == 2 && args[0].equalsIgnoreCase("remove")){
            if (player.hasPermission("toneko.command.remove")) {
                if (confirmMap.getOrDefault(player, false)) {
                    //创建数据文件实例
                    File dataFile = new File("plugins/toNeko/nekos.yml");
                    // 加载数据文件
                    YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                    //判断玩家是否为主人
                    if (data.getString(args[1] + ".owner") != null) {
                        if (data.getString(args[1] + ".owner").equals(player.getName())) {
                            //获取被删除的猫娘对象
                            Player neko = Bukkit.getPlayer(args[1]);
                            if (neko != null) {
                                chatPrefix.subPrivatePrefix(neko, "§a猫娘");
                                create.setNullValue(dataFile, args[1] + ".owner");
                                create.setNullValue(dataFile, args[1] + ".aliases");
                                create.setValue(args[1] + ".xp", 0, dataFile);
                                player.sendMessage("§a你已经成功删除了猫娘§6" + args[1]);
                            } else {
                                player.sendMessage("§c猫娘不存在或不在线");
                            }
                        } else {
                            player.sendMessage("§c你不是" + args[1] + "的主人!");
                        }
                    }
                    confirmMap.remove(player);  // 执行完毕后移除确认状态
                    return true;
                } else {
                    // 发出确认提醒
                    player.sendMessage("§c请再次输入该命令以确认执行: /toneko remove " + args[1]);
                    confirmMap.put(player, true);
                    return false;
                }
            } else {
                player.sendMessage("§c你没有执行该命令的权限!");
            }
        }else if(args.length == 2 && args[0].equalsIgnoreCase("xp")){
            if(player.hasPermission("toneko.command.xp")) {
                //创建数据文件实例
                File dataFile = new File("plugins/toNeko/nekos.yml");
                // 加载数据文件
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                //判断玩家是否为主人
                if (data.getString(args[1] + ".owner") != null) {
                    if (data.getString(args[1] + ".owner").equals(player.getName())) {
                        int xp = data.getInt(args[1] + ".xp");
                        player.sendMessage("§a你与§e" + args[1] + "§b的好感经验为§e" + xp);
                    } else {
                        player.sendMessage("§c你不是" + args[1] + "的主人!");
                    }
                } else {
                    if (data.getString(args[1] + ".owner") != null) {
                        if (data.getString(args[1] + ".owner").equals(player.getName())) {
                            create.createNewKey(args[1] + "." + "xp", player.getName(), data, dataFile);
                        }
                    } else {
                        player.sendMessage("§c你不是" + args[1] + "的主人!");
                    }
                }
            }else {
                player.sendMessage("§c你没有执行该命令的权限!");
            }
        }else if(args.length == 4 && args[0].equalsIgnoreCase("aliases")){
            if(player.hasPermission("toneko.command.aliases")){
                //创建数据文件实例
                File dataFile = new File("plugins/toNeko/nekos.yml");
                // 加载数据文件
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                //判断玩家是否为主人
                if (data.getString(args[1] + ".owner") != null) {
                    if (data.getString(args[1] + ".owner").equals(player.getName())) {
                        //添加别名
                        if(args[2].equalsIgnoreCase("add")){
                            if(data.getList(args[1] + ".aliases") != null){
                                //获取别名
                                List<String> aliases = data.getStringList(args[1] + ".aliases");
                                //将新别名添加到别名
                                aliases.add(args[3]);
                                create.setValue(args[1] + ".aliases",aliases,dataFile);
                                player.sendMessage("成功设置别名" + args[3]);
                            } else {
                                //如果没有则创建值
                                List<String> aliases = new ArrayList<>();
                                aliases.add(args[3]);
                                create.createNewKey(args[1]+".aliases",aliases,data,dataFile);
                                player.sendMessage("成功设置别名" + args[3]);
                            }
                        } else if(args[2].equalsIgnoreCase("remove")){
                            //获取别名
                            List<String> aliases = data.getStringList(args[1] + ".aliases");
                            //判断别名是否存在
                            if(aliases.contains(args[3])) {
                                aliases.remove(args[3]);
                                create.setValue(args[1] + ".aliases",aliases,dataFile);
                                player.sendMessage("已成功删除别名"+args[3]);
                            }else {player.sendMessage("别名" + args[3] +"不存在");}
                        }else {player.sendMessage("§c你的命令/neko "+args[0]+" "+args[1]+"有误，请确认输入的是add或remove");}
                    } else {
                        player.sendMessage("§c你不是" + args[1] + "的主人!");
                    }
                } else {
                        player.sendMessage("§c你不是" + args[1] + "的主人!");
                }
            }else {
                player.sendMessage("§c你没有执行该命令的权限!");
            }
        }else if(args[0].equalsIgnoreCase("item2")) {
            getstick.getStick2(player);
        }
        else {
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
            completions.add("aliases");
            return completions;
        }
        return Collections.emptyList();
    }
    //祭献操作
    private void killPlayer(Player player, String neko) {
        //死亡提示
        Bukkit.getServer().broadcastMessage("§e玩家§6" + player.getName() + "§e为了使§6" + neko + "§e成为猫娘而祭献了自己");
        // 执行玩家死亡操作
        player.setHealth(0);
    }
    public boolean isPlayerOnline(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        return (player != null && player.isOnline());
    }

}
